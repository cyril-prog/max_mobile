package com.max.aiassistant.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.max.aiassistant.model.Note
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gestionnaire de persistance des notes avec SharedPreferences
 */
class NotesPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "notes_prefs"
        private const val KEY_NOTES = "saved_notes"
    }
    
    /**
     * Sauvegarde la liste des notes
     */
    fun saveNotes(notes: List<Note>) {
        val jsonArray = JSONArray()
        notes.forEach { note ->
            val jsonObject = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("timestamp", note.timestamp)
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString(KEY_NOTES, jsonArray.toString()).apply()
    }
    
    /**
     * Charge la liste des notes sauvegardées
     */
    fun loadNotes(): List<Note> {
        val jsonString = prefs.getString(KEY_NOTES, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(jsonString)
            val notes = mutableListOf<Note>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                notes.add(
                    Note(
                        id = jsonObject.getString("id"),
                        title = jsonObject.getString("title"),
                        content = jsonObject.optString("content", ""),
                        timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            notes
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Supprime toutes les notes sauvegardées
     */
    fun clearNotes() {
        prefs.edit().remove(KEY_NOTES).apply()
    }
}
