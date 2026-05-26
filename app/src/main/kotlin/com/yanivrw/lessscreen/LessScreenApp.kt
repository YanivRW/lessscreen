package com.yanivrw.lessscreen

import android.app.Application
import com.yanivrw.lessscreen.data.BlockRepository

class LessScreenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BlockRepository.init(this)
    }
}
