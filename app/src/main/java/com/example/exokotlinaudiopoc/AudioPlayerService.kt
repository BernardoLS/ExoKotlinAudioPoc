package com.example.exokotlinaudiopoc

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.getService
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.Nullable
import com.example.exokotlinaudiopoc.Constants.MEDIA_SESSION_TAG
import com.example.exokotlinaudiopoc.Constants.PLAYBACK_CHANNEL_ID
import com.example.exokotlinaudiopoc.Constants.PLAYBACK_NOTIFICATION_ID
import com.example.exokotlinaudiopoc.Samples.SAMPLES
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.BitmapCallback
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
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
        // Inicialização do player e dos demais metodos da lib com os metodos deprecrated, em breve subo uma branch com os metodos atualizados
        player = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector()) //Constroi um player
        val dataSourceFactory = DefaultDataSourceFactory(
            context,
            Util.getUserAgent(
                context,
                getString(R.string.app_name)
            )
        ) // constroi uma fonte de dados
        val concatenatingMediaSource = ConcatenatingMediaSource() //Inicializa uma fonte de midia concatenavel (playlist de audio/video)
        for (sample in SAMPLES) {
            val mediaSource: MediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(sample.uri)
            concatenatingMediaSource.addMediaSource(mediaSource) //Concatena todos os links dos objetos da array na playlist
        }
        player?.prepare(concatenatingMediaSource) //Alimenta o player com a playlist
        player?.playWhenReady = true

        //Daqui pra baixo configura o gerenciador do player em notificação
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            context,
            PLAYBACK_CHANNEL_ID,
            R.string.playback_channel_name,
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
                    return SAMPLES.get(player.currentWindowIndex).description
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
            }
        )
        playerNotificationManager?.setNotificationListener(object :
            PlayerNotificationManager.NotificationListener { //Captura as interações com a notificação
            override fun onNotificationStarted(
                notificationId: Int,
                notification: Notification
            ) {
                startForeground(notificationId, notification) //Inicializa o serviço em foreground
            }

            override fun onNotificationCancelled(notificationId: Int) {
                stopSelf()
            }
        })
        playerNotificationManager?.setPlayer(player) //define que o player da notificação é o player inicializado anteriormente

        //daqui pra baixo é um copy paste que faz o audiobook ser compativel com o "Ok, google", não testei ainda hehe
        mediaSession = MediaSessionCompat(context, MEDIA_SESSION_TAG)
        mediaSession?.isActive = true
        playerNotificationManager!!.setMediaSessionToken(mediaSession!!.sessionToken)
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
    
}


