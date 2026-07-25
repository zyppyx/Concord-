package com.example.concordmobile_android

import android.content.Context
import com.example.concordmobile_android.data.repository.ConcordRepository

class ConcordContainer(context: Context) {
    val repository: ConcordRepository = ConcordRepository.init(context.applicationContext)
}