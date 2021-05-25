package ee.taltech.alfrol.hw02.ui.states

import ee.taltech.alfrol.hw02.R

data class SessionState(
    val isRunning: Boolean = false,
    val buttonColor: Int = R.color.color_fab_session,
    val buttonIcon: Int = R.drawable.ic_start,
    val error: Int? = null
)
