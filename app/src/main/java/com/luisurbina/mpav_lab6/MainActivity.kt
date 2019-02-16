package com.luisurbina.mpav_lab6

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import java.util.ArrayList
import java.util.Comparator
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.ListView
import com.luisurbina.mpav_lab6.models.Song
import android.content.Intent
import android.content.ComponentName
import com.luisurbina.mpav_lab6.MusicService.MusicBinder
import android.os.IBinder
import android.content.ServiceConnection

import android.content.Context;
import android.view.Menu
import android.view.MenuItem;
import android.view.View
import android.widget.MediaController.MediaPlayerControl


class MainActivity : AppCompatActivity(), MediaPlayerControl {
    companion object {
        private const val MY_PERMISSIONS_REQUEST_READ_CONTACTS = 112
    }

    private var paused = false
    private var playbackPaused = false

    private val songList: ArrayList<Song> = ArrayList()
    private var musicSrv: MusicService? = null
    private var playIntent: Intent? = null
    private var musicBound = false
    private lateinit var controller: MusicController



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val songView: ListView = findViewById<ListView>(R.id.song_list)





        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                val stringArr = arrayListOf<String>()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS
                )

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        getSongList()

        songList.sortWith(Comparator { a, b -> a.getTitle()!!.compareTo(b.getTitle()!!) })

        val songAdt = SongAdapter(this, songList)
        songView.adapter = songAdt


        setController()

    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }
    //connect to the service
    private val musicConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MusicBinder
            //get service
            musicSrv = binder.service
            //pass list
            musicSrv!!.setList(songList)
            musicBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            musicBound = false
        }
    }

    public override fun onStart() {
        super.onStart()
        if (playIntent == null) {
            playIntent = Intent(this, MusicService::class.java)
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE)
            startService(playIntent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_shuffle -> {
                musicSrv!!.setShuffle()
            }
            R.id.action_end -> {
                stopService(playIntent)
                musicSrv = null
                System.exit(0)
            }
        }//shuffle
        return super.onOptionsItemSelected(item)
    }

    override fun isPlaying(): Boolean {
        if (musicSrv != null && musicBound)
            return musicSrv!!.isPng()
        return false
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getDuration(): Int {
        return if (musicSrv != null && musicBound && musicSrv!!.isPng())
            musicSrv!!.getDur()
        else 0
    }

    override fun pause() {
        playbackPaused=true
        musicSrv!!.pausePlayer()
        //controller.show()
    }

    override fun getBufferPercentage(): Int {
       return 0
    }

    override fun seekTo(pos: Int) {
        musicSrv!!.seek(pos)
    }

    override fun getCurrentPosition(): Int {
        return if (musicSrv != null && musicBound && musicSrv!!.isPng())
            musicSrv!!.getPosn()
        else 0
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun start() {
        musicSrv!!.go()
        controller.show()
    }

    override fun getAudioSessionId(): Int {
        return musicSrv!!.getAudioSession()
    }

    override fun canPause(): Boolean {
        return true
    }





    fun getSongList() {
        val musicResolver = contentResolver
        val musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val musicCursor = musicResolver.query(musicUri, null, null, null, null)
        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            val titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE)
            val idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID)
            val artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST)
            //add songs to list
            do {
                val thisId = musicCursor.getLong(idColumn)
                val thisTitle = musicCursor.getString(titleColumn)
                val thisArtist = musicCursor.getString(artistColumn)
                songList!!.add(Song(thisId, thisTitle, thisArtist))
            } while (musicCursor.moveToNext())
        }
    }



    fun songPicked(view: View) {
        musicSrv!!.setSong(Integer.parseInt(view.tag.toString()))
        musicSrv!!.playSong()

    }
    override fun onDestroy() {
        stopService(playIntent)
        musicSrv = null
        super.onDestroy()
    }

    private fun setController() {
        controller = MusicController(this)
        controller.setPrevNextListeners({ playNext() }, { playPrev() })

        controller.setMediaPlayer(this)
        controller.setAnchorView(findViewById(R.id.song_list))
        controller.isEnabled = true
    }

    //play next
    private fun playNext() {
        musicSrv!!.playNext()
        if(playbackPaused){
            setController()
            playbackPaused = false
        }
        controller.show(0)
    }

    //play previous
    private fun playPrev() {
        musicSrv!!.playPrev()
        if(playbackPaused){
            setController()
            playbackPaused=false
        }
        controller.show(0)
    }

    override fun onPause() {
        super.onPause()
        paused=true
    }

    override fun onResume() {
        super.onResume()
        if(paused){
            setController()
            paused=false
        }
    }

    override fun onStop() {
        controller.hide()
        super.onStop()
    }




}
