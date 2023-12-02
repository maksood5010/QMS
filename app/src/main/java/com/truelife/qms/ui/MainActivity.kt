package com.truelife.qms.ui

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.gson.GsonBuilder
import com.truelife.qms.MyApplication
import com.truelife.qms.R
import com.truelife.qms.adapter.TokenAdapter
import com.truelife.qms.tts.TTS
import com.truelife.qms.utils.Constants
import com.truelife.qms.utils.Constants.pwd
import com.truelife.qms.utils.Constants.username
import com.truelife.qms.utils.MediaResponse
import com.truelife.qms.utils.QueueItem
import com.truelife.qms.utils.QueueLine
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit


class MainActivity : FragmentActivity() {
	val tvDrName: TextView by lazy { findViewById(R.id.tvDrName) }
	val roomNo: TextView by lazy { findViewById(R.id.roomNo) }
	val tokenNo: TextView by lazy { findViewById(R.id.tokenNo) }
	val header_scroll: HorizontalScrollView by lazy { findViewById(R.id.header_scroll) }
	val headerInfo: TextView by lazy { findViewById(R.id.tvName) }
	val rv: RecyclerView by lazy { findViewById(R.id.rvToken) }
	val llCurrentQ: LinearLayout by lazy { findViewById(R.id.llCurrentQ) }
	val tts:TTS by lazy { TTS(this@MainActivity) }

	val queueList: ArrayList<QueueItem> = arrayListOf()
	val subList: ArrayList<QueueItem> = arrayListOf()


	private val compositeDisposable = CompositeDisposable()

	private val tokenAdapter: TokenAdapter by lazy { TokenAdapter(subList) }


	override fun onDestroy() {
		super.onDestroy()
		compositeDisposable.dispose()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		Log.d(TAG, "onCreate: test")
		tts.setup()
//		try {
//			val x =Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//			startActivityForResult(x,200)
//		} catch (anfe: ActivityNotFoundException) {
//
//		}

		connectMQTT()
		header_scroll.setOnTouchListener { _, _ -> true }
		headerInfo.startAnimation(AnimationUtils.loadAnimation(this, R.anim.sliding_text))


		rv.adapter = tokenAdapter
		val tts: TextToSpeech = TextToSpeech(this) { }
//			YoYo.with(Techniques.SlideInLeft)
//					  .repeat(-1)
//					  .duration(3000)
//					  .playOn(tokenBg)
//			YoYo.with(Techniques.SlideInLeft)
//					  .repeat(-1)
//					  .duration(3000)
//					  .playOn(roomBg)

		Log.d("TAG", "onInit: tts.voices ${tts.voices}")
		val videoView = findViewById<VideoView>(R.id.videoView)
		val videoUri: Uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.test)
		videoView.setVideoURI(videoUri)
		videoView.setOnCompletionListener {
			videoView.start()
		}
		videoView.start()
	}

	fun splitQueueItems(queueItems: ArrayList<QueueItem>): List<List<QueueItem>> {
		val maxItemsPerList = 10
		val x = queueItems.chunked(maxItemsPerList).map { it.toMutableList() }
		Log.d(TAG, "splitQueueItems: queueItems ${x.size}  ")
		x.map {
			it.add(0, getEmptyQueue())
		}
		return x
	}

	fun getEmptyQueue(): QueueItem {
		return QueueItem("", "", "", "", "", "", "", "")
	}
	private fun isConnected(): Boolean {
		var result = false
		val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
			if (capabilities != null) {
				result = when {
					capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(
						NetworkCapabilities.TRANSPORT_VPN) -> true

					else -> false
				}
			}
		} else {
			val activeNetwork = cm.activeNetworkInfo
			if (activeNetwork != null) {
				// connected to the internet
				result = when (activeNetwork.type) {
					ConnectivityManager.TYPE_WIFI,
					ConnectivityManager.TYPE_MOBILE,
					ConnectivityManager.TYPE_VPN,
					-> true

					else -> false
				}
			}
		}
		return result
	}

	private fun connectMQTT() {
		if (isConnected()){
			Log.d(TAG, "connectMQTT: No Internet")

		}

		val mqttClient = MyApplication.mqttClient
		mqttClient.connect(username, pwd, object : IMqttActionListener {
			override fun onSuccess(asyncActionToken: IMqttToken?) {
				Log.d(this.javaClass.name, "Connection success")
				subscribe(Constants.queue,false)
				subscribe(Constants.mediaQueue,true)
			}

			override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
				Log.d(this.javaClass.name, "Connection failure: ${exception.toString()}")

			}
		}, object : MqttCallback {
			override fun messageArrived(topic: String?, message: MqttMessage?) {
				val msg = "Receive messaged Test: ${message.toString()} from topic: $topic"
				Log.d(this.javaClass.name, msg)
				onMessageArrived(topic, message.toString())
			}

			override fun connectionLost(cause: Throwable?) {
				Log.d(this.javaClass.name, "Connection lost ${cause.toString()}")
			}

			override fun deliveryComplete(token: IMqttDeliveryToken?) {
				Log.d(this.javaClass.name, "Delivery complete")
			}
		})
	}

	fun subscribe(topic: String, onSuccess: Boolean) {
		val mqttClient = MyApplication.mqttClient
		if (mqttClient.isConnected()) {
			mqttClient.subscribe(topic, 1, object : IMqttActionListener {
				override fun onSuccess(asyncActionToken: IMqttToken?) {
					if (onSuccess){
						val msg = "Subscribed to: $topic"
						Log.d(this.javaClass.name, msg)
						val json = JSONObject()
						json.put("reference", Constants.REFERENCE)
						publish(Constants.get_queue, json.toString())
					}
				}

				override fun onFailure(
					asyncActionToken: IMqttToken?,
					exception: Throwable?,
				) {
					Log.d(this.javaClass.name, "Failed to subscribe: $topic")

				}
			})
		} else {
			Log.d(this.javaClass.name, "Impossible to subscribe, no server connected")

		}
	}

	fun publish(topic: String, message: String) {
		val mqttClient = MyApplication.mqttClient
		if (mqttClient.isConnected()) {
			mqttClient.publish(topic, message, 1, false, object : IMqttActionListener {
				override fun onSuccess(asyncActionToken: IMqttToken?) {
					val msg = "Publish message: $message to topic: $topic"
					Log.d(this.javaClass.name, msg)

				}

				override fun onFailure(
					asyncActionToken: IMqttToken?, exception: Throwable?,
				) {
					Log.d(this.javaClass.name, "Failed to publish message to topic")

				}
			})
		} else {
			Log.d(this.javaClass.name, "Impossible to publish, no server connected")
		}
	}

	private var currentIndex = 0

	val TAG = this.javaClass.simpleName

	private fun startSound(filename: String, onComplete: MediaPlayer.OnCompletionListener) {
		var afd: AssetFileDescriptor? = null
		try {
			afd = resources.assets.openFd(filename)
		} catch (e: IOException) {
			e.printStackTrace()
		}
		val player = MediaPlayer()
		try {
			assert(afd != null)
			player.setDataSource(afd!!.fileDescriptor, afd.startOffset, afd.length)
		} catch (e: IOException) {
			e.printStackTrace()
		}
		try {
			player.prepare()
		} catch (e: IOException) {
			e.printStackTrace()
		}
		player.start()
		player.setOnCompletionListener(onComplete)
	}

	private fun onMessageArrived(topic: String?, message: String?) {
		Log.d(TAG, "onMessageArrived: Test")
		when (topic) {
			Constants.queue -> {
				Log.d(TAG, "onMessageArrived: topic : $topic")
				val queueLine: QueueLine = GsonBuilder().create().fromJson(message, QueueLine::class.java)

				if(queueLine.QueueLine.isEmpty()){
					subList.clear()
					tokenAdapter.submitList(subList)
					llCurrentQ.visibility= View.GONE
					return
				}else{
					llCurrentQ.visibility= View.VISIBLE
				}
				queueList.clear()
				queueList.addAll(queueLine.QueueLine.reversed())
				Log.d(TAG, "onMessageArrived: submitList ")


				val firstItem = queueList.firstOrNull() ?: return
				tokenNo.text = firstItem.token
				roomNo.text = firstItem.room_number
				tvDrName.text = firstItem.en_name

				tts.speakOutArabic(firstItem)

				val splitQueueItems = splitQueueItems(queueList)


				if (splitQueueItems.size <= 1) {
					subList.clear()
					subList.addAll(queueList)
					tokenAdapter.submitList(subList)
				} else {
					var currentIndex = 0
					subList.clear()
					subList.addAll(splitQueueItems.first())
					tokenAdapter.submitList(subList)

					compositeDisposable.clear()
					val disposable =
						Flowable.interval(10, TimeUnit.SECONDS).onBackpressureDrop().observeOn(AndroidSchedulers.mainThread()).subscribe { tick ->
							Log.d(TAG, "MessageArrived: Observable.interval $tick")

							YoYo.with(Techniques.Flash).repeat(0).duration(2000).onEnd {
								currentIndex++
								var temp = splitQueueItems.getOrNull(currentIndex)
								if (temp == null) {
									currentIndex = 0
									temp = splitQueueItems[currentIndex]
								}
								subList.clear()
								subList.addAll(temp)
								tokenAdapter.submitList(subList)
							}.playOn(rv)

						}

					compositeDisposable.add(disposable)
				}


			}
			Constants.mediaQueue->{
				val mediaResponse: MediaResponse = GsonBuilder().create().fromJson(message, MediaResponse::class.java)
				headerInfo.text = mediaResponse.institution_name

			}
		}
	}


}