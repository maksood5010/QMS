package com.truelife.qms.tts

import android.app.Activity
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.speech.tts.Voice.LATENCY_VERY_LOW
import android.speech.tts.Voice.QUALITY_VERY_HIGH
import android.util.Log
import android.widget.Toast
import com.truelife.qms.utils.QueueItem
import java.util.Locale


class TTS(
	private val activity: Activity,
) : TextToSpeech.OnInitListener {

	private val tts: TextToSpeech = TextToSpeech(activity, this)
	private val ttsEn: TextToSpeech = TextToSpeech(activity, this)

	override fun onInit(i: Int) {

		if (i == TextToSpeech.SUCCESS) {

			val localeAR = Locale("ar")
			val localeUS = Locale("en")

			val result: Int = tts.setLanguage(localeAR)
			val result1: Int = ttsEn.setLanguage(localeUS)
			tts.setVoice(Voice(localeAR.toLanguageTag(),
				localeAR, QUALITY_VERY_HIGH, LATENCY_VERY_LOW, false, null))
			ttsEn.setVoice(Voice(localeUS.toLanguageTag(),
				localeAR, QUALITY_VERY_HIGH, LATENCY_VERY_LOW, false, null))
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Toast.makeText(activity, "This Language is not supported", Toast.LENGTH_SHORT).show()
			}

		} else {
			Toast.makeText(activity, "Initilization Failed!", Toast.LENGTH_SHORT).show()

		}
	}



	fun speakOut(token: QueueItem) {
		ttsEn.speak("Token Number ${token.token} Room Number ${token.room_number} Doctor Name ${token.en_name}", TextToSpeech.QUEUE_ADD, null, null)
	}
	fun speakOutArabic(token: QueueItem) {
//		tts.setSpeechRate(0.9f)
		Log.d("TAG", "speakOutArabic: called now")
		val a = " رقم التذكرة" + token.token
		val b = "رقم الغرفة" + token.room_number
		val c = " اسم الطبيب" + token.ar_name
		tts.speak("$a$b$c", TextToSpeech.QUEUE_ADD, null, null)
		speakOut(token)
	}
}