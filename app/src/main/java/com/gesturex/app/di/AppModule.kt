package com.gesturex.app.di

import android.content.Context
import com.gesturex.app.data.camera.CameraGestureDetector
import com.gesturex.app.data.sensors.SensorGestureDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCameraGestureDetector(
        @ApplicationContext context: Context
    ): CameraGestureDetector = CameraGestureDetector(context)

    @Provides
    @Singleton
    fun provideSensorGestureDetector(
        @ApplicationContext context: Context
    ): SensorGestureDetector = SensorGestureDetector(context)
}
