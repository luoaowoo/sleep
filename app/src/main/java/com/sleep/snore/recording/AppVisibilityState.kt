package com.sleep.snore.recording

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

interface AppVisibilityState {
    val isAppVisible: Boolean
}

@Singleton
class AppVisibilityTracker @Inject constructor() : AppVisibilityState {
    private val startedActivityCount = AtomicInteger(0)
    private val registered = AtomicBoolean(false)

    override val isAppVisible: Boolean
        get() = startedActivityCount.get() > 0

    fun register(application: Application) {
        if (!registered.compareAndSet(false, true)) return
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    startedActivityCount.incrementAndGet()
                }

                override fun onActivityStopped(activity: Activity) {
                    startedActivityCount.updateAndGet { count -> (count - 1).coerceAtLeast(0) }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )
    }
}
