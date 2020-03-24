package com.example.exokotlinaudiopoc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.example.exokotlinaudiopoc.AudioPlayerService.LocalBinder
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var mIntent: Intent
    private lateinit var mService: AudioPlayerService
    private var mBound = false

    private val mConnection = object : ServiceConnection { //Responsavel por fazer conexão do serviço do audioPlayer com a view do player
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocalBinder
            mService = binder.getBinder()
            mBound = true
            initializePlayer()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }
    }

    private fun initializePlayer() { // Inicializa um player para conectar com a view
        if (mBound) {
            val player = mService.getPlayerInstance()
            player_view.player = player
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mIntent = Intent(this, AudioPlayerService::class.java) // Prepara um intent para inicializar um Serviço Foreground/Background
        Util.startForegroundService(this, intent) //Inicializa o Serviço em Foreground que permite que o app execute o Serviço na notificação de controle
        player_view.useController = true;
        player_view.showController();
        player_view.controllerAutoShow = true;
        player_view.controllerHideOnTouch = false;
    }

    override fun onStart() {
        super.onStart()
        bindService(mIntent, mConnection, Context.BIND_AUTO_CREATE) //Conecta de fato a Activity com o Serviço do player
        initializePlayer()
    }

}
