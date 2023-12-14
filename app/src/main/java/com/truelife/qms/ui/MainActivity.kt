package com.truelife.qms.ui

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.video.VideoSize
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
	val roomNo: TextView by lazy { findViewById(R.id.roomNo) }
	val tokenNo: TextView by lazy { findViewById(R.id.tokenNo) }
	val header_scroll: HorizontalScrollView by lazy { findViewById(R.id.header_scroll) }
	val headerInfo: TextView by lazy { findViewById(R.id.tvName) }
	val rv: RecyclerView by lazy { findViewById(R.id.rvToken) }
	val llCurrentQ: LinearLayout by lazy { findViewById(R.id.llCurrentQ) }
	val tts:TTS by lazy { TTS(this@MainActivity) }
	val videoView  by lazy { findViewById<PlayerView>(R.id.videoView) }
	val imageView by lazy {  findViewById<ImageView>(R.id.imageView)}
	val viewFlipper by lazy {  findViewById<ViewFlipper>(R.id.viewSwicher) }

	private lateinit var mHttpDataSourceFactory: HttpDataSource.Factory
	private lateinit var mDefaultDataSourceFactory: DefaultDataSource.Factory
	private lateinit var mCacheDataSourceFactory: DataSource.Factory

	private var exoPlayer: ExoPlayer? =null

	val queueList: ArrayList<QueueItem> = arrayListOf()
	val subList: ArrayList<QueueItem> = arrayListOf()


	private val compositeDisposable = CompositeDisposable()

	private val tokenAdapter: TokenAdapter by lazy { TokenAdapter(subList) }


	override fun onDestroy() {
		exoPlayer?.stop()
		exoPlayer?.release()
		tts.onDestroy()
		compositeDisposable.dispose()
		super.onDestroy()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		Log.d(TAG, "onCreate: test")
		setUpExoPlayer()
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
		val videoUri1: Uri = Uri.parse("https://res.cloudinary.com/dgwddohoe/video/upload/v1685251957/samples/elephants.mp4")
		val videoUri2: Uri = Uri.parse("https://res.cloudinary.com/dgwddohoe/video/upload/v1685251960/samples/cld-sample-video.mp4")
		val videoUri3: Uri = Uri.parse("https://res.cloudinary.com/dgwddohoe/video/upload/v1701773668/pexels-ana-benet-8243345_720p_l8lx7d.mp4")
//		val videoUri4: Uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.test)
		val imageUri = "https://res.cloudinary.com/dgwddohoe/image/upload/v1688592429/background_oei42q.jpg"
		val files = arrayListOf(videoUri3, videoUri3,videoUri3)

		val medialist: List<MediaSource> = files.map {
			ProgressiveMediaSource.Factory(mCacheDataSourceFactory).createMediaSource(MediaItem.fromUri(it))
		}

		var currentIndex=1
		exoPlayer?.setMediaSources(medialist)
		exoPlayer?.prepare()
		exoPlayer?.repeatMode = Player.REPEAT_MODE_ALL
		exoPlayer?.addListener(object : Player.Listener {
			override fun onVideoSizeChanged(videoSize: VideoSize) {
				super.onVideoSizeChanged(videoSize)
				val isVertical = videoSize.height > videoSize.width
				Log.d(TAG, "screen Width::: heightPixels: ${resources.displayMetrics.heightPixels}:: widthPixels::${resources.displayMetrics.widthPixels}")
				Log.d(TAG, "onVideoSizeChanged: height > width ${videoSize.height} > ${ videoSize.width }")
				if (isVertical){
					Log.d(TAG, "onVideoSizeChanged: its Vertical Image")
					Log.d(TAG, "onVideoSizeChanged: ${videoView.width}")
					videoView.resizeMode = RESIZE_MODE_FIT

				}
			}

			override fun onPlaybackStateChanged(playbackState: Int) {

				//On Media Queue Ends
				Log.d(TAG, "onPlaybackStateChanged: playbackState == ExoPlayer :: $playbackState")
				if (playbackState == ExoPlayer.STATE_ENDED) {

				}
			}

			override fun onPositionDiscontinuity(
				oldPosition: Player.PositionInfo,
				newPosition: Player.PositionInfo,
				reason: Int
			) {
				//THIS METHOD GETS CALLED FOR EVERY NEW SOURCE THAT IS PLAYED
//				val latestWindowIndex: Int = exoPlayer?.currentMediaItemIndex?:0
//				if (latestWindowIndex != 0) {
//				}
				if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION){
					playNext()
					exoPlayer?.pause()
					Glide.with(this@MainActivity).load(imageUri)
							  .placeholder(R.drawable.progress_animation)
							  .centerCrop()
							  .diskCacheStrategy(DiskCacheStrategy.ALL)
							  .into(imageView)
					viewFlipper.showNext()
					val r:Runnable = Runnable {
						currentIndex++
						viewFlipper.showPrevious()
						exoPlayer?.play()
						if (currentIndex >= files.size) {
							currentIndex = 0
						}
						exoPlayer?.seekTo(currentIndex, C.TIME_UNSET)
					}

					Handler(Looper.getMainLooper()).postDelayed(r,10000)
				}


			}
		})
		exoPlayer?.seekTo(0, 0)
		exoPlayer?.playWhenReady = true


//		videoView.setOnPreparedListener { mediaPlayer ->
//			val videoRatio = mediaPlayer.videoWidth / mediaPlayer.videoHeight.toFloat()
//			val screenRatio = videoView.width / videoView.height.toFloat()
//			val scaleX = videoRatio / screenRatio
//			if (scaleX >= 1f) {
//				videoView.scaleX = scaleX
//			} else {
//				videoView.scaleY = 1f / scaleX
//			}
//		}
	}

	private fun playNext() {

	}
	fun isVideoVertical(videoPath: String): Boolean {
		val retriever = MediaMetadataRetriever()
		try {
			// Set the video file to the retriever
			retriever.setDataSource(videoPath)

			// Get the video height and width
			val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
			val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
			Log.d(TAG, "isVideoVertical: width $width ::: height ::: $height")
			// Determine if the video is vertical
			return height > width
		} catch (e: Exception) {
			e.printStackTrace()
		} finally {
			retriever.release()
		}
		return false
	}


	private fun setUpExoPlayer() {
		val cacheEvictor = NoOpCacheEvictor()
		val databaseProvider = StandaloneDatabaseProvider(this)
		val cache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)

		mHttpDataSourceFactory = DefaultHttpDataSource.Factory()
				  .setAllowCrossProtocolRedirects(true)

		this.mDefaultDataSourceFactory = DefaultDataSource.Factory(
			this, mHttpDataSourceFactory
		)

		mCacheDataSourceFactory = CacheDataSource.Factory()
				  .setCache(cache)
				  .setUpstreamDataSourceFactory(mHttpDataSourceFactory)
				  .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

		exoPlayer = ExoPlayer.Builder(applicationContext).setMediaSourceFactory(DefaultMediaSourceFactory(mCacheDataSourceFactory)).build()
		videoView.player =exoPlayer


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

				if (queueLine.announce.toBoolean()){
					tts.speakOutArabic(firstItem)
				}

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