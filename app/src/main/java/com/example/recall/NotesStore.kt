package com.example.recall

import android.content.Context
import org.json.JSONArray

object NotesStore {
    private const val PREFS = "recall_notes"
    private const val NOTES = "notes"
    private const val LAST = "last_note"
    private const val CYCLE = "cycle_order"
    private const val CURRENT = "current"
    private const val HEADS_UP = "heads_up"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getNotes(context: Context): List<String> {
        val raw = prefs(context).getString(NOTES, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun saveNotes(context: Context, notes: List<String>) {
        val arr = JSONArray()
        notes.forEach { arr.put(it) }
        // Notes changed, so start a fresh shuffle cycle next time.
        prefs(context).edit()
            .putString(NOTES, arr.toString())
            .remove(CYCLE)
            .remove(LAST)
            .apply()
    }

    fun current(context: Context): String = prefs(context).getString(CURRENT, "") ?: ""

    fun setCurrent(context: Context, text: String) {
        prefs(context).edit().putString(CURRENT, text).apply()
    }

    fun headsUp(context: Context): Boolean = prefs(context).getBoolean(HEADS_UP, true)

    fun setHeadsUp(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(HEADS_UP, enabled).apply()
    }

    /** Picks the next note: every note is shown once per shuffled cycle. */
    fun pickNext(context: Context): String {
        val p = prefs(context)
        val notes = getNotes(context)
        if (notes.isEmpty()) {
            setCurrent(context, "")
            return ""
        }
        if (notes.size == 1) {
            setCurrent(context, notes[0])
            return notes[0]
        }

        var order = p.getString(CYCLE, "")
            ?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        var cursor = p.getInt(LAST, -1)

        val cycleIsValid = order.size == notes.size &&
            order.toSet() == notes.indices.toSet() &&
            cursor < order.size - 1

        if (!cycleIsValid) {
            val previous = current(context)
            var fresh = notes.indices.shuffled()
            // Don't show the same note twice in a row across cycles.
            if (notes[fresh[0]] == previous) fresh = fresh.drop(1) + fresh[0]
            order = fresh
            cursor = -1
        }

        cursor++
        val chosen = notes[order[cursor]]
        p.edit()
            .putString(CYCLE, order.joinToString(","))
            .putInt(LAST, cursor)
            .putString(CURRENT, chosen)
            .apply()
        return chosen
    }
}
