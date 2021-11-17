package dev.nmullaney.tesladashboard

import android.app.Application
import androidx.annotation.CallSuper
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.internal.managers.BroadcastReceiverComponentManager.generatedComponent
import dagger.hilt.internal.UnsafeCasts

@HiltAndroidApp
class TeslaDashboardApplication : Application()

