package com.truelife.qms.tts

import android.app.Activity
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.speech.tts.Voice.LATENCY_VERY_LOW
import android.speech.tts.Voice.QUALITY_VERY_HIGH
import android.widget.Toast
import java.util.Locale


class NaturalTTS(
	private val activity: Activity
) : TextToSpeech.OnInitListener {

	private val tts: TextToSpeech = TextToSpeech(activity, this)

	override fun onInit(i: Int) {

		if (i == TextToSpeech.SUCCESS) {

			val localeUS = Locale.US

			val result: Int = tts.setLanguage(localeUS)
			tts.setVoice(Voice(localeUS.toLanguageTag(),
				localeUS, QUALITY_VERY_HIGH, LATENCY_VERY_LOW, false, null))
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Toast.makeText(activity, "This Language is not supported", Toast.LENGTH_SHORT).show()
			}

		} else {
			Toast.makeText(activity, "Initilization Failed!", Toast.LENGTH_SHORT).show()

		}
	}



	fun speakOut(token: String, onComplete: TextToSpeech.OnUtteranceCompletedListener) {

		tts.setSpeechRate(0.5f)
		tts.speak(token, TextToSpeech.QUEUE_ADD, null, null)
		tts.setOnUtteranceCompletedListener(onComplete)
	}
}