package ee.taltech.alfrol.hw02.api

import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

class AuthorizedJsonObjectRequest(
    method: Int,
    url: String,
    jsonRequest: JSONObject?,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener,
    private val token: String,
) : JsonObjectRequest(method, url, jsonRequest, listener, errorListener) {

    override fun getHeaders(): MutableMap<String, String> {
        val headers: MutableMap<String, String> = mutableMapOf()
        headers["Authorization"] = "Bearer $token"
        return headers
    }
}