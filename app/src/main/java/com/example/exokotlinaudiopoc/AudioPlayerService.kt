package com.example.exokotlinaudiopoc

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.Nullable
import com.example.exokotlinaudiopoc.Constants.MEDIA_SESSION_TAG
import com.example.exokotlinaudiopoc.Constants.PLAYBACK_CHANNEL_ID
import com.example.exokotlinaudiopoc.Constants.PLAYBACK_NOTIFICATION_ID
import com.example.exokotlinaudiopoc.Samples.SAMPLES
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.BitmapCallback
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util


class AudioPlayerService : Service() {
    private var player: SimpleExoPlayer? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var mediaSession: MediaSessionCompat? = null
    private var mediaSessionConnector: MediaSessionConnector? = null
    val context: Context = this
    private val mBinder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        //Classe utilizada no bind do player do serviço com o player da view
        fun getBinder(): AudioPlayerService {
            return this@AudioPlayerService
        }
    }

    override fun onCreate() {
        super.onCreate()
        startPlayer()
    }

    private fun startPlayer() {
        // Inicialização do player e dos demais metodos

        val dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, getString(R.string.app_name))) // constroi uma fonte de dados
        player = SimpleExoPlayer.Builder(context).setTrackSelector(DefaultTrackSelector(context)).build() //Constroi um player
        player?.playWhenReady = true

        //Daqui pra baixo configura o gerenciador do player em notificação
        buildSingleMediaSource(dataSourceFactory) //se esse estiver sendo chamado comentar o buildConcatMediaSources
        //  buildConcatMediaSources(dataSourceFactory) //se esse estiver sendo chamado comentar o buildSingleMediaSource
        configNotificationManager()
        configAudioFocus()
        configMediaSession()
    }

    private fun buildSingleMediaSource(dataSourceFactory: DefaultDataSourceFactory) {
        val uri = Uri.parse("https://poc-audio.s3.amazonaws.com/20000-leagues-under-the-sea.mp3")
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
        player?.prepare(mediaSource) //Alimenta o player com a playlist
    }

    private fun buildConcatMediaSources(dataSourceFactory: DefaultDataSourceFactory) {
        val concatenatingMediaSource = ConcatenatingMediaSource() //Inicializa uma fonte de midia concatenavel (playlist de audio/video)

        for (sample in SAMPLES) {
            val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(sample.uri)
            concatenatingMediaSource.addMediaSource(mediaSource) //Concatena todos os links dos objetos da array na playlist
        }
        player?.prepare(concatenatingMediaSource) //Alimenta o player com a playlist
    }

    private fun configNotificationManager() {
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            context,
            PLAYBACK_CHANNEL_ID,
            R.string.playback_channel_name,
            R.string.playback_channel_description,
            PLAYBACK_NOTIFICATION_ID,
            object : MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): String {
                    return SAMPLES[player.currentWindowIndex].title
                }

                @Nullable
                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    return null
                }

                @Nullable
                override fun getCurrentContentText(player: Player): String? {
                    return SAMPLES[player.currentWindowIndex].description
                }

                @Nullable
                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: BitmapCallback
                ): Bitmap? {
                    return Samples.getBitmap(
                        context, SAMPLES[player.currentWindowIndex].bitmapResource
                    )
                }
            },
            object : PlayerNotificationManager.NotificationListener { //Captura as interações com a notificação
                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    super.onNotificationCancelled(notificationId, dismissedByUser)
                    if  (dismissedByUser) {
                        stopSelf()
                    }
                }

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    super.onNotificationPosted(notificationId, notification, ongoing)
                    startForeground(notificationId, notification) //Inicializa o serviço em foreground
                }
            }
        )
        playerNotificationManager?.setPlayer(player) //define que o player da notificação é o player inicializado anteriormente
    }

    private fun configMediaSession() { //copy paste que faz o audiobook ser compativel com o "Ok, google", não testei ainda hehe
        mediaSession = MediaSessionCompat(context, MEDIA_SESSION_TAG)
        mediaSession?.isActive = true
        playerNotificationManager?.setMediaSessionToken(mediaSession?.sessionToken ?: return)
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector?.setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
            override fun getMediaDescription(
                player: Player,
                windowIndex: Int
            ): MediaDescriptionCompat {
                return Samples.getMediaDescription(context, SAMPLES[windowIndex])
            }
        })
        mediaSessionConnector?.setPlayer(player, null)
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSessionConnector?.setPlayer(null, null)
        playerNotificationManager?.setPlayer(null)
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaSession?.release()
        mediaSessionConnector?.setPlayer(null, null)
        playerNotificationManager?.setPlayer(null)
        player?.release()
        player = null
    }

    private fun configAudioFocus() { //Configura o Foco do audio para modo de discurso pausando e resumindo automaticamente a reprodução quando recebida uma notificação
        val audioAtribute = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_SPEECH)
            .build()
        player?.audioAttributes = audioAtribute
    }

    fun getPlayerInstance() : SimpleExoPlayer? {
        if (player == null) {
            startPlayer()
        }
        return player
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    inner class PlaybackStateListener : Player.EventListener { // descobre estado do player
        override fun onPlayerStateChanged(
            playWhenReady: Boolean,
            playbackState: Int
        ) {
            var stateString: String
            when (playbackState) {
                ExoPlayer.STATE_IDLE -> stateString = "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> stateString = "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> stateString = "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> {
                    stateString = "ExoPlayer.STATE_ENDED     -"
                    stateString = "UNKNOWN_STATE             -"
                }
                else -> stateString = "UNKNOWN_STATE             -"
            }
        }
    }
}


