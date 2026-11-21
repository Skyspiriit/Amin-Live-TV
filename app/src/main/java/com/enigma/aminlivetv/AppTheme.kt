package com.enigma.aminlivetv

import android.content.Context
import android.graphics.Color

// Colour themes the viewer can pick from the home screen. Every screen reads the current palette,
// so a change applies everywhere. The choice is saved on the TV.
internal data class Palette(
    val key: String,
    val name: String,
    val background: Int,
    val backgroundDeep: Int,
    val surface: Int,
    val selected: Int,
    val tileStart: Int,
    val tileEnd: Int,
    val accent: Int,
    val ink: Int = Color.rgb(247, 250, 244),
    val muted: Int = Color.rgb(190, 202, 198),
) {
    val artwork: Int get() = when (key) {
        "lapis", "violet" -> R.drawable.fantasy_palace
        "ruby", "saffron", "golden" -> R.drawable.fantasy_desert
        else -> R.drawable.fantasy_garden
    }
    val sceneName: String get() = when (key) {
        "emerald" -> "باغ زمرد"
        "turquoise" -> "رویای فیروزه"
        "lapis" -> "قصر لاجورد"
        "violet" -> "کهکشان بنفشه"
        "ruby" -> "شفق یاقوت"
        "golden" -> "سینمای طلایی"
        else -> "کویر زعفران"
    }
}

internal object AppTheme {
    // «سینمای طلایی» is Amin Player's charcoal and gold. It is the default for a TV that never picked
    // a theme; a saved choice is kept.
    val palettes = listOf(
        Palette("golden", "طلایی", Color.rgb(20, 17, 20), Color.rgb(11, 9, 11), Color.rgb(42, 35, 31),
            Color.rgb(78, 59, 31), Color.rgb(64, 48, 27), Color.rgb(31, 25, 23), Color.rgb(242, 181, 68),
            ink = Color.rgb(250, 244, 232), muted = Color.rgb(208, 194, 170)),
        Palette("emerald", "زمرد", Color.rgb(10, 27, 23), Color.rgb(4, 13, 11), Color.rgb(22, 48, 42),
            Color.rgb(31, 74, 63), Color.rgb(20, 84, 66), Color.rgb(11, 40, 33), Color.rgb(126, 226, 184)),
        Palette("turquoise", "فیروزه", Color.rgb(7, 26, 34), Color.rgb(3, 12, 17), Color.rgb(18, 48, 59),
            Color.rgb(26, 75, 91), Color.rgb(16, 82, 102), Color.rgb(9, 38, 50), Color.rgb(94, 214, 232)),
        Palette("lapis", "لاجورد", Color.rgb(10, 16, 36), Color.rgb(4, 7, 18), Color.rgb(24, 35, 70),
            Color.rgb(38, 58, 114), Color.rgb(36, 60, 138), Color.rgb(17, 27, 64), Color.rgb(143, 180, 255)),
        Palette("violet", "بنفشه", Color.rgb(20, 12, 34), Color.rgb(9, 5, 17), Color.rgb(43, 26, 69),
            Color.rgb(66, 40, 106), Color.rgb(84, 50, 140), Color.rgb(36, 22, 64), Color.rgb(201, 166, 255)),
        Palette("ruby", "یاقوت", Color.rgb(28, 10, 16), Color.rgb(14, 4, 8), Color.rgb(58, 22, 33),
            Color.rgb(92, 34, 51), Color.rgb(120, 32, 56), Color.rgb(52, 15, 27), Color.rgb(255, 143, 166)),
        Palette("saffron", "زعفران", Color.rgb(28, 19, 7), Color.rgb(14, 9, 3), Color.rgb(58, 40, 16),
            Color.rgb(90, 62, 23), Color.rgb(122, 84, 26), Color.rgb(54, 37, 12), Color.rgb(255, 199, 106)),
    )

    @Volatile var current: Palette = palettes.first()
        private set

    fun load(context: Context) {
        val key = prefs(context).getString(KEY, null)
        current = palettes.firstOrNull { it.key == key } ?: palettes.first()
    }

    fun select(context: Context, palette: Palette) {
        current = palette
        prefs(context).edit().putString(KEY, palette.key).apply()
    }

    // The startup intro and its sound, both on unless the viewer turns them off in «تنظیمات».
    fun introAnimation(context: Context) = prefs(context).getBoolean(INTRO_ANIMATION, true)
    fun introSound(context: Context) = prefs(context).getBoolean(INTRO_SOUND, true)
    fun setIntro(context: Context, animation: Boolean, sound: Boolean) {
        prefs(context).edit().putBoolean(INTRO_ANIMATION, animation).putBoolean(INTRO_SOUND, sound).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    private const val KEY = "theme"
    private const val INTRO_ANIMATION = "intro-animation"
    private const val INTRO_SOUND = "intro-sound"
}
