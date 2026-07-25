package com.example.concordmobile_android.data.remote

import org.json.JSONObject

fun JSONObject.optIntAny(vararg names: String): Int {
    for (name in names) {
        if (has(name) && !isNull(name)) return optInt(name)
    }
    return 0
}

fun JSONObject.optStringAny(vararg names: String): String {
    for (name in names) {
        if (has(name) && !isNull(name)) return optString(name)
    }
    return ""
}

fun JSONObject.optLongAny(vararg names: String): Long {
    for (name in names) {
        if (has(name) && !isNull(name)) return optLong(name)
    }
    return 0L
}
