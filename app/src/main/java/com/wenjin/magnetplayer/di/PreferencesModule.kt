package com.wenjin.magnetplayer.di

import com.wenjin.magnetplayer.database.MpvExDatabase
import com.wenjin.magnetplayer.preferences.AdvancedPreferences
import com.wenjin.magnetplayer.preferences.AppearancePreferences
import com.wenjin.magnetplayer.preferences.AudioPreferences
import com.wenjin.magnetplayer.preferences.BrowserPreferences
import com.wenjin.magnetplayer.preferences.DecoderPreferences
import com.wenjin.magnetplayer.preferences.FoldersPreferences
import com.wenjin.magnetplayer.preferences.GesturePreferences
import com.wenjin.magnetplayer.preferences.PlayerPreferences
import com.wenjin.magnetplayer.preferences.SettingsManager
import com.wenjin.magnetplayer.preferences.SubtitlesPreferences
import com.wenjin.magnetplayer.preferences.preference.AndroidPreferenceStore
import com.wenjin.magnetplayer.preferences.preference.PreferenceStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val PreferencesModule =
  module {
    single { AndroidPreferenceStore(androidContext()) }.bind(PreferenceStore::class)

    single { AppearancePreferences(get()) }
    singleOf(::PlayerPreferences)
    singleOf(::GesturePreferences)
    singleOf(::DecoderPreferences)
    singleOf(::SubtitlesPreferences)
    singleOf(::AudioPreferences)
    singleOf(::AdvancedPreferences)
    single { BrowserPreferences(get(), androidContext()) }
    singleOf(::FoldersPreferences)
    singleOf(::SettingsManager)
  }
