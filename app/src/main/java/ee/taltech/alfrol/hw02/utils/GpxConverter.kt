package ee.taltech.alfrol.hw02.utils

import android.content.Context
import android.location.Location
import android.net.Uri
import java.io.FileOutputStream

object GpxConverter {

    private const val HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>" +
                "<gpx " +
                "xmlns=\"http://www.topografix.com/GPX/1/1\" " +
                "creator=\"GPS Sport Map\" " +
                "version=\"1.1\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  " +
                "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">" +
                "<trk>\n<trkseg>"
    private const val FOOTER = "</trkseg></trk></gpx>"

    /**
     * Save the given location points as a GPX file to the given uri.
     *
     * @param context Context to use for file opening.
     * @param uri Where to store the file.
     * @param points Location points to save.
     */
    fun save(context: Context, uri: Uri, points: List<Location>) {
        val data = convert(points)

        runCatching {
            context.contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { file ->
                    file.write(data.toByteArray())
                }
            }
        }
    }

    /**
     * Convert the given list of [Location] objects to a GPX string.
     */
    private fun convert(points: List<Location>): String {
        var segments = HEADER
        for (point in points) {
            segments += "<trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">"
            segments += "<time> ${UIUtils.timeMillisToIsoOffset(point.time)} </time></trkpt>\n"
        }
        segments += FOOTER

        return segments
    }
}