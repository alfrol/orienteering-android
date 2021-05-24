package ee.taltech.alfrol.hw02.ui.other

data class RegisterFormState(
    val firstNameError: Int? = null,
    val lastNameError: Int? = null,
    val emailError: Int? = null,
    val passwordError: Int? = null,
    val passwordConfirmationError: Int? = null,
    val touched: Boolean = false,
    val isDataValid: Boolean = false
)
