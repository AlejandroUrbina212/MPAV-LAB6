package com.luisurbina.mpav_lab6

import android.app.*
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri;
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.util.Log;
import android.view.MenuItem
import com.luisurbina.mpav_lab6.models.Song
import java.util.*


class MusicService: Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {
    companion object {
        private const val NOTIFY_ID = 1
    }
    private var player: MediaPlayer? = null
    //song list
    private var songs: ArrayList<Song>? = null
    //current position
    private var songPosn: Int = 0
    private var songTitle = ""
    private val musicBind: IBinder = MusicBinder()
    private var shuffle = false
    private var rand: Random? = null


    override fun onBind(intent: Intent?): IBinder? {
        return musicBind

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onPrepared(mp: MediaPlayer?) {
        //start playback
        mp!!.start()
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "My Background Service")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        val notIntent = Intent(this, MainActivity::class.java)
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendInt = PendingIntent.getActivity(this, 0,
            notIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, channelId )
        val notification = notificationBuilder.setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentText(songTitle)
            .setOngoing(true)
            .setContentIntent(pendInt)
            .setContentTitle("Music Player")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(NOTIFY_ID, notification)

        //Code for notifications extracted from https://www.techotopia.com/index.php/An_Android_8_Notifications_Kotlin_Tutorial
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mp!!.reset()
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if(player!!.currentPosition == 0){
            mp!!.reset()
            playNext()
        }
    }

    override fun onDestroy() {
        stopForeground(true)
    }
    fun getAudioSession():Int{
        return player!!.audioSessionId

    }

    override fun onCreate() {
        //create the service
        super.onCreate()
        //initialize position
        songPosn = 0
        //create player
        player = MediaPlayer()
        initMusicPlayer()
        rand = Random()
    }
    fun setShuffle() {
        shuffle = !shuffle
    }

    private fun initMusicPlayer() {
        player = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        }

        player!!.setOnPreparedListener(this)
        player!!.setOnCompletionListener(this)
        player!!.setOnErrorListener(this)
    }

    inner class MusicBinder : Binder() {
        internal val service: MusicService
            get() = this@MusicService
    }

    fun setList(theSongs: ArrayList<Song>) {
        songs = theSongs
    }


    override fun onUnbind(intent: Intent?): Boolean {
        player!!.stop()
        player!!.release()
        return false
    }

    fun playSong() {
        player!!.reset()
        //get song
        val playSong = songs!![songPosn]
        //get id
        val currSong = playSong.songID
        songTitle = playSong.songTitle
        //set uri
        val trackUri = ContentUris.withAppendedId(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            currSong
        )
        try {
            player!!.setDataSource(applicationContext, trackUri)
        } catch (e: Exception) {
            Log.e("MUSIC SERVICE", "Error setting data source", e)
        }

        player!!.prepareAsync()

    }

    fun setSong(songIndex: Int) {
        songPosn = songIndex
    }


    fun playPrev(){
        songPosn--
        if(songPosn == 0) songPosn = songs!!.size-1
        playSong()
    }

    fun playNext(){
        if(shuffle){
            var newSong = songPosn
            while(newSong == songPosn){
                newSong = rand!!.nextInt(songs!!.size)
            }
            songPosn = newSong
        }
        else{
            songPosn++
            if(songPosn == songs!!.size) songPosn=0
        }
        playSong()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(id: String, name: String): String{
        val myChannel = NotificationChannel(id,
            name, NotificationManager.IMPORTANCE_NONE)
        myChannel.enableLights(true)
        myChannel.lightColor = Color.RED
        myChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(myChannel)
        return id
    }

    fun getPosn(): Int {
        return player!!.currentPosition
    }

    fun getDur(): Int {
        return player!!.duration
    }

    fun isPng(): Boolean {
        return player!!.isPlaying()
    }

    fun pausePlayer() {
        player!!.pause()
    }

    fun seek(posn: Int) {
        player!!.seekTo(posn)
    }

    fun go() {
        player!!.start()
    }




}