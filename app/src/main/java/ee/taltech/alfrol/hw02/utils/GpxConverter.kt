package ee.taltech.alfrol.hw02.utils

import android.content.Context
import android.location.Location
import android.net.Uri
import java.io.FileOutputStream

object GpxConverter {

    private const val HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
                "<gpx " +
                "xmlns=\"http://www.topografix.com/GPX/1/1\" " +
                "creator=\"GPS Sport Map\" " +
                "version=\"1.1\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  " +
                "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n"
    private const val DIVIDER = "<trk>\n<trkseg>\n"
    private const val FOOTER = "</trkseg>\n</trk>\n</gpx>\n"

    /**
     * Save the given location points as a GPX file to the given uri.
     *
     * @param context Context to use for file opening.
     * @param uri Where to store the file.
     * @param pathPoints Location points to save.
     * @param checkpoints Checkpoints to save.
     */
    fun save(context: Context, uri: Uri, pathPoints: List<Location>, checkpoints: List<Location>) {
        val data = convert(pathPoints, checkpoints)

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
     *
     * @param pathPoints In the GPX file will be the trkpt element.
     * @param checkpoints In the GPX file will be the wpt element.
     */
    private fun convert(pathPoints: List<Location>, checkpoints: List<Location>): String {
        var segments = ""

        checkpoints.forEach { segments += getGpxSegment(it, "wpt") }
        segments += DIVIDER
        pathPoints.forEach { segments += getGpxSegment(it, "trkpt") }

        return HEADER + segments + FOOTER
    }

    /**
     * Get the GPX segment depending on the element tag.
     */
    private fun getGpxSegment(location: Location, tag: String) =
        "<$tag lat=\"${location.latitude}\" lon=\"${location.longitude}\">\n" +
                "<time>${UIUtils.timeMillisToIsoOffset(location.time)}</time>\n</$tag>\n"
}