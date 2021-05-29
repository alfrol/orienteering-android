package ee.taltech.alfrol.hw02.utils

import ee.taltech.alfrol.hw02.data.model.LocationPoint
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object GpxConverter {

    private const val HEADER =
        "\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\" standalone=\\\"no\\\" ?>" +
                "<gpx " +
                "xmlns=\\\"http://www.topografix.com/GPX/1/1\\\" " +
                "creator=\\\"GPS Sport Map\\\" " +
                "version=\\\"1.0\\\" " +
                "xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\"  " +
                "xsi:schemaLocation=\\\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\\\">" +
                "<trk>\\n\";"
    private const val FOOTER = "</trkseg></trk></gpx>"

    fun convert(points: List<LocationPoint>): String {
        var segments = HEADER
        for (point in points) {
            segments += "<trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">"
            segments += "<time> ${formatTime(point.recordedAt)} </time></trkpt>\n"
        }
        segments += FOOTER

        return segments
    }

    private fun formatTime(ms: Long) = OffsetDateTime.ofInstant(
        Instant.ofEpochMilli(ms),
        ZoneId.systemDefault()
    ).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}