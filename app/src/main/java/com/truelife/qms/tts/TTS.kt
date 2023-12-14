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

	private lateinit var tts: TextToSpeech
	private lateinit var ttsEn: TextToSpeech

	fun setup(){
		tts = TextToSpeech(activity, this)
		ttsEn = TextToSpeech(activity, this)
	}
	fun onDestroy() {
		tts.stop()
		ttsEn.stop()
		tts.shutdown()
		ttsEn.shutdown()
	}

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
			Toast.makeText(activity, "Initialization Failed!", Toast.LENGTH_SHORT).show()

		}
	}



	fun speakOut(token: QueueItem) {
		ttsEn.speak("Token Number ${token.token} Please Proceed to Counter Number ${token.room_number}", TextToSpeech.QUEUE_ADD, null, null)
	}
	fun speakOutArabic(token: QueueItem) {
		/*tts.setSpeechRate(0.9f)*/
		Log.d("TAG", "speakOutArabic: called now")

		val a = " رقم التذكرة" + token.token
		val b = "الرجاء التوجه إلى الكاونتر رقم" + token.room_number

		tts.speak("$a$b", TextToSpeech.QUEUE_ADD, null, null)
		speakOut(token)
	}
}