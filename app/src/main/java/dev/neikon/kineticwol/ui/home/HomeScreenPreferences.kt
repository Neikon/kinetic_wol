package dev.neikon.kineticwol.ui.home

import android.content.Context

class HomeScreenPreferences(
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isHeroCardDismissed(): Boolean =
        preferences.getBoolean(KEY_HERO_CARD_DISMISSED, false)

    fun setHeroCardDismissed(dismissed: Boolean) {
        preferences.edit()
            .putBoolean(KEY_HERO_CARD_DISMISSED, dismissed)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "home_screen_preferences"
        private const val KEY_HERO_CARD_DISMISSED = "hero_card_dismissed"
    }
}
