package com.me.wifiswitcher

import android.animation.ValueAnimator
import android.graphics.drawable.Animatable
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView

private const val TILE_START_SCALE = 0.6f
private const val TILE_IN_MS = 420L
private const val OVERSHOOT_TENSION = 1.6f
private const val GLYPH_DELAY_MS = 150L
private const val RIPPLE_DELAY_MS = 700L
private const val RIPPLE_MS = 600L
private const val RIPPLE_START_ALPHA = 0.35f
private const val RIPPLE_END_SCALE = 1.8f
private const val SPLASH_HOLD_MS = 1150L
private const val SPLASH_FADE_MS = 240L
private const val TILE_EXIT_SCALE = 1.08f

/**
 * Plays the first-launch splash: the tile pops in, the glyph draws, a ripple pulses out, then the overlay fades.
 * Hides the overlay at once when it is not a first launch or the system has animations off.
 * @param splash full-screen overlay, hidden when the splash ends
 * @param ripple square behind the tile that pulses outward once
 * @param tile blue tile that scales and fades in from [TILE_START_SCALE]
 * @param glyph Wi-Fi icon whose draw-in animation is started after [GLYPH_DELAY_MS]
 * @param firstLaunch true only when the activity starts with no saved state
 */
fun playSplash(splash: View, ripple: View, tile: View, glyph: ImageView, firstLaunch: Boolean) {
    // === NOTE: === Respects the system "remove animations" setting, so no splash is shown at all
    if (!firstLaunch || !ValueAnimator.areAnimatorsEnabled()) {
        splash.visibility = View.GONE
        return
    }
    animateTileIn(tile)
    splash.postDelayed({ (glyph.drawable as? Animatable)?.start() }, GLYPH_DELAY_MS)
    splash.postDelayed({ startRipple(ripple) }, RIPPLE_DELAY_MS)
    splash.postDelayed({ exitSplash(splash, tile) }, SPLASH_HOLD_MS)
}

private fun animateTileIn(tile: View) {
    tile.scaleX = TILE_START_SCALE
    tile.scaleY = TILE_START_SCALE
    tile.alpha = 0f
    tile.animate()
        .scaleX(1f)
        .scaleY(1f)
        .alpha(1f)
        .setDuration(TILE_IN_MS)
        .setInterpolator(OvershootInterpolator(OVERSHOOT_TENSION))
        .start()
}

private fun startRipple(ripple: View) {
    ripple.alpha = RIPPLE_START_ALPHA
    ripple.animate()
        .scaleX(RIPPLE_END_SCALE)
        .scaleY(RIPPLE_END_SCALE)
        .alpha(0f)
        .setDuration(RIPPLE_MS)
        .setInterpolator(DecelerateInterpolator())
        .start()
}

private fun exitSplash(splash: View, tile: View) {
    tile.animate()
        .scaleX(TILE_EXIT_SCALE)
        .scaleY(TILE_EXIT_SCALE)
        .setDuration(SPLASH_FADE_MS)
        .start()
    splash.animate()
        .alpha(0f)
        .setDuration(SPLASH_FADE_MS)
        .withEndAction { splash.visibility = View.GONE }
        .start()
}
