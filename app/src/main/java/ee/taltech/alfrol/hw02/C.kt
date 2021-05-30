package ee.taltech.alfrol.hw02

object C {

    const val IS_USER_LOGGED_IN_KEY = "is_user_logged_in"

    private const val API_BASE = "https://sportmap.akaver.com/api/v1.0"
    const val API_LOGIN_URL = "$API_BASE/account/login"
    const val API_REGISTRATION_URL = "$API_BASE/account/register"
    const val API_SESSIONS_URL = "$API_BASE/GpsSessions"
    const val API_LOCATIONS_URL = "$API_BASE/GpsLocations"

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

    const val JSON_LATITUDE_KEY = "latitude"
    const val JSON_LONGITUDE_KEY = "longitude"
    const val JSON_ACCURACY_KEY = "accuracy"
    const val JSON_ALTITUDE_KEY = "altitude"
    const val JSON_VERTICAL_ACCURACY = "verticalAccuracy"
    const val JSON_GPS_SESSION_ID = "gpsSessionId"
    const val JSON_GPS_LOCATION_TYPE_ID = "gpsLocationTypeId"

    const val NOTIFICATION_CHANNEL_ID = "gps_sport_map"
    const val NOTIFICATION_CHANNEL_NAME = "GPS Sport Map Channel"
    const val NOTIFICATION_ID = 1

    const val ACTION_START_SERVICE = "START_SERVICE"
    const val ACTION_STOP_SERVICE = "STOP_SERVICE"
    const val ACTION_GET_CURRENT_LOCATION = "GET_CURRENT_LOCATION"
    const val ACTION_ADD_CHECKPOINT = "ADD_CHECKPOINT"
    const val ACTION_ADD_WAYPOINT = "ADD_WAYPOINT"
    const val ACTION_START_CHECKPOINT_STOPWATCH = "START_CHECKPOINT_STOPWATCH"
    const val ACTION_START_WAYPOINT_STOPWATCH = "START_WAYPOINT_STOPWATCH"

    const val STOPWATCH_TYPE_KEY = "stopwatch_type"
    const val LOCATION_REQUEST_KEY = "location_request"
    const val LOCATION_ACTION_COMMAND = "location_action_command"

    const val DEFAULT_LOCATION_UPDATE_INTERVAL = 10000L
    const val DEFAULT_LOCATION_UPDATE_FASTEST_INTERVAL = 5000L
    const val DEFAULT_MAP_ZOOM = 17.0f
    const val DEFAULT_MAP_CAMERA_ANIMATION_DURATION = 2000
    const val DEFAULT_POLYLINE_WIDTH = 10.0f
    const val DEFAULT_POLYLINE_COLOR = R.color.primary
    const val DEFAULT_PREVIEW_TRACK_PADDING = 50

    const val LOC_TYPE_ID = "00000000-0000-0000-0000-000000000001"
    const val WP_TYPE_ID = "00000000-0000-0000-0000-000000000002"
    const val CP_TYPE_ID = "00000000-0000-0000-0000-000000000003"

    const val LOCATION_PERMISSION_REQUEST_CODE = 1
    const val STORAGE_WRITE_PERMISSION_REQUEST_CODE = 2

    const val PROVIDER_FUSED = "fused"
}