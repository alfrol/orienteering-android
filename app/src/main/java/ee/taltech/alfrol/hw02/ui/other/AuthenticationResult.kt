package ee.taltech.alfrol.hw02.ui.other

data class AuthenticationResult(
    val success: Boolean = false,
    val error: Int? = null
)