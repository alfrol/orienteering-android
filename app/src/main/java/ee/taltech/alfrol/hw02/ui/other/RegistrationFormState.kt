package ee.taltech.alfrol.hw02.ui.other

data class RegistrationFormState(
    val firstNameError: Int? = null,
    val lastNameError: Int? = null,
    val emailError: Int? = null,
    val passwordError: Int? = null,
    val passwordConfirmationError: Int? = null,
    val isDataValid: Boolean = false,
)
