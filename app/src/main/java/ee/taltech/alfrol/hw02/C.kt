package ee.taltech.alfrol.hw02

object C {

    const val IS_USER_LOGGED_IN_KEY = "is_user_logged_in"

    private const val API_BASE = "https://sportmap.akaver.com/api/v1.0"
    const val API_LOGIN_URL = "$API_BASE/account/login"
    const val API_REGISTRATION_URL = "$API_BASE/account/register"
    const val API_SESSIONS_URL = "$API_BASE/GpsSessions"

    const val JSON_FIRST_NAME_KEY = "firstName"
    const val JSON_LAST_NAME_KEY = "lastName"
    const val JSON_EMAIL_KEY = "email"
    const val JSON_PASSWORD_KEY = "password"
    const val JSON_TOKEN_KEY = "token"

    const val JSON_NAME_KEY = "name"
    const val JSON_DESCRIPTION_KEY = "description"
    const val JSON_RECORDED_AT_KEY = "recordedAt"
    const val JSON_PACE_MIN_KEY = "paceMin"
    const val JSON_PACE_MAX_KEY = "paceMax"
    const val JSON_ID_KEY = "id"

    const val NOTIFICATION_CHANNEL_ID = "gps_sport_map"
    const val NOTIFICATION_CHANNEL_NAME = "GPS Sport Map Channel"
    const val NOTIFICATION_ID = 1

    const val ACTION_START_SERVICE = "START_SERVICE"
    const val ACTION_STOP_SERVICE = "STOP_SERVICE"
    const val ACTION_GET_CURRENT_LOCATION = "GET_CURRENT_LOCATION"

    const val STOPWATCH_TYPE_KEY = "stopwatch_type"

    const val STOPWATCH_TOTAL = 0
    const val STOPWATCH_CHECKPOINT = 1
    const val STOPWATCH_WAYPOINT = 2

    const val DEFAULT_LOCATION_UPDATE_INTERVAL = 10000L
    const val DEFAULT_LOCATION_UPDATE_FASTEST_INTERVAL = 5000L
}