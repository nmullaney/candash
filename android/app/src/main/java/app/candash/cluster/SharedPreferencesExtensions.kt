package app.candash.cluster

import android.content.SharedPreferences

fun SharedPreferences.setPref(name: String, value: Float) {
    with(this.edit()) {
        putFloat(name, value)
        apply()
    }
}

fun SharedPreferences.setBooleanPref(name: String, value: Boolean) {
    with(this.edit()) {
        putBoolean(name, value)
        apply()
    }
}

fun SharedPreferences.prefContains(name: String): Boolean {
    return this.contains(name)
}

fun SharedPreferences.getPref(name: String): Float {
    return this.getFloat(name, 0f)
}

fun SharedPreferences.getBooleanPref(name: String): Boolean {
    return this.getBoolean(name, false)
}