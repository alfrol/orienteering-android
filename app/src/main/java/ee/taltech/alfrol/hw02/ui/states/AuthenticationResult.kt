package ee.taltech.alfrol.hw02.ui.states

data class AuthenticationResult(
    val success: Boolean = false,
    val error: Int? = null
)