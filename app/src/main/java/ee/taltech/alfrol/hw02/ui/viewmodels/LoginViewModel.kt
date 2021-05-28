package ee.taltech.alfrol.hw02.ui.viewmodels

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
import ee.taltech.alfrol.hw02.data.repositories.UserRepository
import ee.taltech.alfrol.hw02.ui.states.AuthenticationResult
import ee.taltech.alfrol.hw02.ui.states.LoginFormState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val restHandler: RestHandler,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private var _loginFormState = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginFormState

    private var _loginResult = MutableLiveData<AuthenticationResult>()
    val loginResult: LiveData<AuthenticationResult> = _loginResult

    /**
     * Try to log the user in.
     */
    fun login(email: String, password: String) {
        val requestBody = JSONObject()
        requestBody.put(C.JSON_EMAIL_KEY, email)
        requestBody.put(C.JSON_PASSWORD_KEY, password)

        val loginRequest = JsonObjectRequest(Request.Method.POST, C.API_LOGIN_URL, requestBody,
            { response ->
                val token = response.getString(C.JSON_TOKEN_KEY)

                // Save user id and API token to the datastore for later use.
                viewModelScope.launch {
                    // TODO: This assumes the user is already in the database
                    //  but if someone tries to log in from other phone, then this
                    //  will not work since user will not be in the database
                    val user = userRepository.findByEmail(email).first()
                    settingsManager.saveToken(token)
                    settingsManager.saveLoggedInUser(user.id)

                    _loginResult.postValue(AuthenticationResult(success = true))
                }
            },
            {
                _loginResult.value = AuthenticationResult(error = R.string.error_login_failed)
            })
        restHandler.cancelPendingRequests()
        restHandler.addRequest(loginRequest)
    }

    /**
     * Validate that the new login details are valid.
     * This method only checks the text, not the correctness of the details.
     */
    fun loginDataChanged(email: String, password: String) {
        var isValid = true

        if (!isEmailValid(email)) {
            _loginFormState.value = LoginFormState(emailError = R.string.error_invalid_email)
            isValid = false
        }
        if (!isPasswordValid(password)) {
            _loginFormState.value = LoginFormState(passwordError = R.string.error_empty_password)
            isValid = false
        }

        if (isValid) {
            _loginFormState.value = LoginFormState(isDataValid = true)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotBlank()
    }
}