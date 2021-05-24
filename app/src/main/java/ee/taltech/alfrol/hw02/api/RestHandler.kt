package ee.taltech.alfrol.hw02.api

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

class RestHandler(context: Context) {

    companion object {
        private val TAG: String = RestHandler::class.java.simpleName
    }

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context)
    }

    fun <T> addRequest(request: Request<T>) {
        request.tag = TAG
        requestQueue.add(request)
    }

    fun cancelPendingRequests() {
        requestQueue.cancelAll(TAG)
    }
}