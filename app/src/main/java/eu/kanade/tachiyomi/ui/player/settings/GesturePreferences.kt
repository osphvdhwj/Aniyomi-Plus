package eu.kanade.tachiyomi.ui.player.settings

import eu.kanade.tachiyomi.ui.player.SingleActionGesture
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class GesturePreferences(
    private val preferenceStore: PreferenceStore,
) {
    // ... existing preferences ...
    fun gestureVolumeBrightness() = preferenceStore.getBoolean("pref_gesture_volume_brightness", true)
    fun swapVolumeBrightness() = preferenceStore.getBoolean("pref_swap_volume_and_brightness", false)
    fun gestureHorizontalSeek() = preferenceStore.getBoolean("pref_gesture_horizontal_seek", true)
    fun showSeekBar() = preferenceStore.getBoolean("pref_show_seekbar", false)
    fun defaultIntroLength() = preferenceStore.getInt("pref_default_intro_length", 85)
    fun skipLengthPreference() = preferenceStore.getInt("pref_skip_length_preference", 10)
    fun playerSmoothSeek() = preferenceStore.getBoolean("pref_player_smooth_seek", false)

    fun leftDoubleTapGesture() = preferenceStore.getEnum("pref_left_double_tap", SingleActionGesture.Seek)
    fun centerDoubleTapGesture() = preferenceStore.getEnum("pref_center_double_tap", SingleActionGesture.PlayPause)
    fun rightDoubleTapGesture() = preferenceStore.getEnum("pref_right_double_tap", SingleActionGesture.Seek)

    fun mediaPreviousGesture() = preferenceStore.getEnum("pref_media_previous", SingleActionGesture.Switch)
    fun mediaPlayPauseGesture() = preferenceStore.getEnum("pref_media_playpause", SingleActionGesture.PlayPause)
    fun mediaNextGesture() = preferenceStore.getEnum("pref_media_next", SingleActionGesture.Switch)

    // --- FIX: USE STRING FOR CUSTOM INPUT ---
    fun defaultHoldSpeed() = preferenceStore.getString("pref_default_hold_speed_v2", "2.0")
    fun customHoldSpeeds() = preferenceStore.getString("pref_custom_hold_speeds", "0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0")
    fun allowGestures() = preferenceStore.getBoolean("pref_allow_gestures_in_panels", false)
}
