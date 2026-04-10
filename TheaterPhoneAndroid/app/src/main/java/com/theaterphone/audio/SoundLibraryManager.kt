package com.theaterphone.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import com.theaterphone.data.model.SoundItem

/**
 * Manages imported audio files — import, persist, play by name.
 * Port of iOS SoundLibraryManager.
 */
class SoundLibraryManager(private val context: Context) {

    private val _sounds = MutableStateFlow<List<SoundItem>>(emptyList())
    val sounds: StateFlow<List<SoundItem>> = _sounds

    private val _nowPlaying = MutableStateFlow<String?>(null)
    val nowPlaying: StateFlow<String?> = _nowPlaying

    private var player: MediaPlayer? = null

    /** Temporary URI from file picker, used by SettingsScreen */
    var pendingUri: Uri? = null

    private val audioDir: File
        get() {
            val dir = File(context.filesDir, "AudioLibrary")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val storageFile: File
        get() = File(context.filesDir, "sound_library.json")

    init {
        loadSounds()
    }

    fun importFile(uri: Uri, name: String) {
        try {
            val id = UUID.randomUUID().toString()
            val ext = context.contentResolver.getType(uri)?.substringAfter("/") ?: "mp3"
            val fileName = "$id.$ext"
            val destFile = File(audioDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val item = SoundItem(id = id, name = name, fileName = fileName)
            _sounds.value = _sounds.value + item
            saveSounds()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteSound(id: String) {
        val item = _sounds.value.find { it.id == id } ?: return
        File(audioDir, item.fileName).delete()
        _sounds.value = _sounds.value.filter { it.id != id }
        saveSounds()
    }

    fun play(name: String) {
        val item = _sounds.value.find { it.name.equals(name, ignoreCase = true) }
        if (item == null) return

        stop()
        val file = File(audioDir, item.fileName)
        if (!file.exists()) return

        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    _nowPlaying.value = null
                    it.release()
                    player = null
                }
            }
            _nowPlaying.value = item.name
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
        _nowPlaying.value = null
    }

    private fun saveSounds() {
        val array = JSONArray()
        for (item in _sounds.value) {
            array.put(JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("fileName", item.fileName)
            })
        }
        storageFile.writeText(array.toString())
    }

    private fun loadSounds() {
        if (!storageFile.exists()) return
        try {
            val array = JSONArray(storageFile.readText())
            val items = mutableListOf<SoundItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                items.add(SoundItem(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    fileName = obj.getString("fileName")
                ))
            }
            _sounds.value = items
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
