package ee.taltech.alfrol.hw02.utils

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

class CreateDocumentContract : ActivityResultContracts.CreateDocument() {

    override fun createIntent(context: Context, input: String): Intent {
        return super.createIntent(context, input).apply { type = "text/xml" }
    }
}