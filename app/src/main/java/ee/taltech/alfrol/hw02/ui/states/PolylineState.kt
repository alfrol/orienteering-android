package ee.taltech.alfrol.hw02.ui.states

import androidx.annotation.ColorRes

data class PolylineState(
    @ColorRes
    val colorSlow: Int,

    @ColorRes
    val colorNormal: Int,

    @ColorRes
    val colorFast: Int,
    val width: Float
)
