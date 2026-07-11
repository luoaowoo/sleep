package com.sleep.snore.recording

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecordingControllerModule {

    @Binds
    @Singleton
    abstract fun bindRecordingController(controller: AndroidRecordingController): RecordingController

    @Binds
    @Singleton
    abstract fun bindAppVisibilityState(tracker: AppVisibilityTracker): AppVisibilityState
}
