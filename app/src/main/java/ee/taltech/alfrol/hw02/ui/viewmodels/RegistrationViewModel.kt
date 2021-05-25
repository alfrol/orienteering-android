package ee.taltech.alfrol.hw02.ui.viewmodels

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.api.RestHandler
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.data.dao.UserDao
import ee.taltech.alfrol.hw02.data.model.User
import ee.taltech.alfrol.hw02.ui.states.AuthenticationResult
import ee.taltech.alfrol.hw02.ui.states.RegistrationFormState
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val userDao: UserDao,
    private val restHandler: RestHandler,
    private val settingsManager: SettingsManager
) : ViewModel() {

    companion object {
        private val INCORRECT_PASSWORD_REGEX =
            Regex("^(.{0,6}|[^0-9]*|[^A-Z]*|[^a-z]*|[^_]*)\$").pattern
    }

    private var _registrationFormState = MutableLiveData<RegistrationFormState>()
    val registrationFormState: LiveData<RegistrationFormState> = _registrationFormState

    private var _registrationResult = MutableLiveData<AuthenticationResult>()
    val registrationResult: LiveData<AuthenticationResult> = _registrationResult

    /**
     * Try to register a new user.
     */
    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ) {
        val requestBody = JSONObject()
        requestBody.put(C.JSON_FIRST_NAME_KEY, firstName)
        requestBody.put(C.JSON_LAST_NAME_KEY, lastName)
        requestBody.put(C.JSON_EMAIL_KEY, email)
        requestBody.put(C.JSON_PASSWORD_KEY, password)

        val registrationRequest =
            JsonObjectRequest(Request.Method.POST, C.API_REGISTRATION_URL, requestBody,
                { response ->
                    val token = response.getString(C.JSON_TOKEN_KEY)

                    // Add user info to the database.
                    // Also save user id and API token to the datastore for later use.
                    viewModelScope.launch {
                        val user = User(firstName = firstName, lastName = lastName, email = email)
                        val userId = userDao.insert(user)

                        settingsManager.saveToken(token)
                        settingsManager.saveLoggedInUser(userId)

                        _registrationResult.postValue(AuthenticationResult(success = true))
                    }
                },
                {
                    _registrationResult.value =
                        AuthenticationResult(error = R.string.error_registration_failed)
                })
        restHandler.cancelPendingRequests()
        restHandler.addRequest(registrationRequest)
    }

    /**
     * Validate that the new registration details are valid.
     * This method only checks the text, not the correctness of the details.
     */
    fun registrationDataChanged(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        passwordConfirmation: String
    ) {
        Log.d("RegistrationViewModel", "registrationDataChanged")
        var isValid = true

        if (!isTextFieldValid(firstName)) {
            _registrationFormState.value =
                RegistrationFormState(firstNameError = R.string.error_empty_required_field)
            isValid = false
        }
        if (!isTextFieldValid(lastName)) {
            _registrationFormState.value =
                RegistrationFormState(lastNameError = R.string.error_empty_required_field)
            isValid = false
        }
        if (!isEmailValid(email)) {
            _registrationFormState.value =
                RegistrationFormState(emailError = R.string.error_invalid_email)
            isValid = false
        }
        if (!isPasswordValid(password)) {
            _registrationFormState.value =
                RegistrationFormState(passwordError = R.string.error_incorrect_password)
            isValid = false
        }
        if (!isPasswordConfirmationValid(password, passwordConfirmation)) {
            _registrationFormState.value =
                RegistrationFormState(passwordConfirmationError = R.string.error_password_match)
            isValid = false
        }

        if (isValid) {
            _registrationFormState.value = RegistrationFormState(isDataValid = true)
        }
    }

    private fun isTextFieldValid(text: String): Boolean {
        return text.isNotBlank()
    }

    private fun isEmailValid(username: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(username).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return !Pattern.matches(INCORRECT_PASSWORD_REGEX, password)
    }

    private fun isPasswordConfirmationValid(
        password: String,
        passwordConfirmation: String
    ): Boolean {
        return password == passwordConfirmation
    }
}