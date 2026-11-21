#!/usr/bin/env python3
"""Drives the app on an Android TV emulator over adb and records what is focused and shown.

Usage: ui.py <step> [args]. Each step prints one line to the report so the run log doubles as
the test record. Screenshots land in $OUT.
"""
import os, re, subprocess, sys, time, xml.etree.ElementTree as ET

OUT = os.environ.get("OUT", "tv-check-output")
os.makedirs(OUT, exist_ok=True)
PKG = "com.enigma.aminlivetv"


def adb(*args, check=True):
    return subprocess.run(["adb", *args], capture_output=True, text=True, check=check).stdout


def dump():
    for _ in range(5):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml", check=False)
        raw = adb("shell", "cat", "/sdcard/ui.xml", check=False)
        if raw.strip().startswith("<?xml"):
            return ET.fromstring(raw[raw.index("<hierarchy"):])
        time.sleep(1)
    raise SystemExit("could not read the screen")


def label(node):
    return node.get("content-desc") or node.get("text") or ""


def bounds(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    return (x1 + x2) / 2, (y1 + y2) / 2


def focused_node(root=None):
    for node in (root or dump()).iter("node"):
        if node.get("focused") == "true":
            return node
    return None


def focused():
    node = focused_node()
    return label(node) if node is not None else ""


def go(name):
    """Moves focus to the item called `name` with D-pad keys only, as a remote would."""
    for _ in range(20):
        root = dump()
        now = focused_node(root)
        target = None
        for node in root.iter("node"):
            if label(node).startswith(name) and node.get("focusable") == "true":
                target = node
                break
        if target is None:
            report(f"FAIL go: no focusable «{name}»")
            return False
        if now is not None and now is target or (now is not None and label(now).startswith(name)):
            return True
        fx, fy = bounds(now) if now is not None else (0, 0)
        tx, ty = bounds(target)
        if abs(ty - fy) > 30:
            key("DPAD_DOWN" if ty > fy else "DPAD_UP")
        else:
            key("DPAD_LEFT" if tx < fx else "DPAD_RIGHT")
    report(f"FAIL go: could not reach «{name}», focus «{focused()}»")
    return False


def texts():
    found = []
    for n in dump().iter("node"):
        for v in (n.get("content-desc"), n.get("text")):
            if v and v not in found:
                found.append(v)
    return found


def find(name):
    nodes = list(dump().iter("node"))
    names = lambda n: [n.get("content-desc") or "", n.get("text") or ""]
    for test in (lambda v: v == name, lambda v: v.startswith(name)):
        for node in nodes:
            if any(test(v) for v in names(node) if v):
                return node
    return None


def tap(name):
    node = find(name)
    if node is None:
        report(f"FAIL tap: «{name}» not on screen; visible: {texts()[:30]}")
        return False
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(3)
    return True


def key(name, times=1):
    for _ in range(times):
        adb("shell", "input", "keyevent", f"KEYCODE_{name}")
        time.sleep(1.2)


def shot(name):
    adb("shell", "screencap", "-p", "/sdcard/s.png")
    adb("pull", "/sdcard/s.png", f"{OUT}/{name}.png")


def report(line):
    print(line, flush=True)
    with open(f"{OUT}/report.txt", "a") as f:
        f.write(line + "\n")


def expect(what, ok, detail=""):
    report(("PASS " if ok else "FAIL ") + what + (f" — {detail}" if detail else ""))


step, *args = sys.argv[1:]
if step == "launch":
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    time.sleep(10)
elif step == "focus":
    report(f"focus: «{focused()}»")
elif step == "key":
    key(args[0], int(args[1]) if len(args) > 1 else 1)
    report(f"key {args[0]}×{args[1] if len(args) > 1 else 1} → focus «{focused()}»")
elif step == "tap":
    tap(args[0])
elif step == "go":
    if go(args[0]):
        report(f"focus reached «{args[0]}»")
elif step == "open":
    # Focus with the D-pad, then OK.
    if go(args[0]):
        key("DPAD_CENTER")
        time.sleep(2)
        report(f"opened «{args[0]}»")
elif step == "shot":
    shot(args[0])
elif step == "expect-focus":
    now = focused()
    expect(f"focus is «{args[0]}»", now.startswith(args[0]), f"actual «{now}»")
elif step == "expect-text":
    visible = texts()
    expect(f"«{args[0]}» visible", any(t.startswith(args[0]) for t in visible), "" if any(t.startswith(args[0]) for t in visible) else str(visible[:30]))
elif step == "expect-no-text":
    visible = texts()
    expect(f"«{args[0]}» not shown", not any(t.startswith(args[0]) for t in visible))
elif step == "order":
    # The section headings must appear top to bottom in this order.
    nodes = [(int(re.findall(r"\d+", n.get("bounds"))[1]), label(n)) for n in dump().iter("node")
             if label(n) in args and n.get("focusable") != "true" and n.get("clickable") != "true"
             and not any(c.get("focusable") == "true" for c in [n])]
    # Skip the top-menu labels, which repeat the section names inside focusable buttons.
    menu = {label(b) for b in dump().iter("node") if b.get("focusable") == "true"}
    nodes = [(y, name) for y, name in nodes if not (name in menu and y < 200)]
    seen = [name for _, name in sorted(nodes)]
    expect("sections in order " + " → ".join(args), seen == list(args), f"found {seen}")
elif step == "texts":
    report("visible: " + " | ".join(texts()))
