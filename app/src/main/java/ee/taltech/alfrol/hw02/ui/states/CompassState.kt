package ee.taltech.alfrol.hw02.ui.states

import ee.taltech.alfrol.hw02.R

data class CompassState(
    val isEnabled: Boolean = false,
    val compassButtonIcon: Int = R.drawable.ic_compass_on
)
