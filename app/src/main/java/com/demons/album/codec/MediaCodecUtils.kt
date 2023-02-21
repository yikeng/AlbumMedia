package com.demons.album.codec

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import com.linkedin.android.litr.utils.MediaFormatUtils
import com.linkedin.android.litr.utils.TranscoderUtils
import java.io.IOException

object MediaCodecUtils {

     val KEY_ROTATION =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) MediaFormat.KEY_ROTATION else "rotation-degrees"
    
     fun getMediaDuration(context: Context, uri: Uri): Long {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(context, uri)
        val durationStr =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        return durationStr!!.toLong()
    }


     fun getInt(mediaFormat: MediaFormat, key: String): Int {
        return getInt(mediaFormat, key, -1)
    }

     fun getInt(mediaFormat: MediaFormat, key: String, defaultValue: Int): Int {
        return if (mediaFormat.containsKey(key)) {
            mediaFormat.getInteger(key)
        } else defaultValue
    }

     fun getLong(mediaFormat: MediaFormat, key: String): Long {
        return if (mediaFormat.containsKey(key)) {
            mediaFormat.getLong(key)
        } else -1
    }

     fun updateTrimConfig(trimConfig: TrimConfig, sourceMedia: SourceMedia) {
        trimConfig.setTrimEnd(sourceMedia.duration)
    }

     fun updateSourceMedia(context: Context, sourceMedia: SourceMedia, uri: Uri) {
        sourceMedia.uri = uri
        sourceMedia.size = TranscoderUtils.getSize(context, uri)
        sourceMedia.duration = getMediaDuration(context, uri) / 1000f
        try {
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(context, uri, null)
            sourceMedia.tracks = ArrayList(mediaExtractor.trackCount)
            for (track in 0 until mediaExtractor.trackCount) {
                val mediaFormat = mediaExtractor.getTrackFormat(track)
                val mimeType = mediaFormat.getString(MediaFormat.KEY_MIME) ?: continue
                if (mimeType.startsWith("video")) {
                    val videoTrack =
                        VideoTrackFormat(track, mimeType)
                    videoTrack.width = getInt(mediaFormat, MediaFormat.KEY_WIDTH)
                    videoTrack.height = getInt(mediaFormat, MediaFormat.KEY_HEIGHT)
                    videoTrack.duration = getLong(mediaFormat, MediaFormat.KEY_DURATION)
                    videoTrack.frameRate = MediaFormatUtils.getFrameRate(mediaFormat, -1).toInt()
                    videoTrack.keyFrameInterval =
                        MediaFormatUtils.getIFrameInterval(mediaFormat, -1).toInt()
                    videoTrack.rotation = getInt(
                        mediaFormat,
                        KEY_ROTATION,
                        0
                    )
                    videoTrack.bitrate = getInt(mediaFormat, MediaFormat.KEY_BIT_RATE)
                    sourceMedia.tracks.add(videoTrack)
                } else if (mimeType.startsWith("audio")) {
                    val audioTrack =
                        AudioTrackFormat(track, mimeType)
                    audioTrack.channelCount = getInt(mediaFormat, MediaFormat.KEY_CHANNEL_COUNT)
                    audioTrack.samplingRate = getInt(mediaFormat, MediaFormat.KEY_SAMPLE_RATE)
                    audioTrack.duration = getLong(mediaFormat, MediaFormat.KEY_DURATION)
                    audioTrack.bitrate = getInt(mediaFormat, MediaFormat.KEY_BIT_RATE)
                    sourceMedia.tracks.add(audioTrack)
                } else {
                    sourceMedia.tracks.add(
                        GenericTrackFormat(
                            track,
                            mimeType
                        )
                    )
                }
            }
        } catch (ex: IOException) {
            Log.e(
                "TAG",
                "Failed to extract sourceMedia",
                ex
            )
        }
        sourceMedia.notifyChange()
    }
}