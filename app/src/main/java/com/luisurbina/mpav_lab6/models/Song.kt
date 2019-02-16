package com.luisurbina.mpav_lab6.models

class Song(val songID: Long, val songTitle: String, val songArtist: String) {
    private var id: Long = songID
    private var title: String? = songTitle
    private var artist: String? = songArtist


    fun getID(): Long {
        return id
    }

    fun getTitle(): String? {
        return title
    }

    fun getArtist(): String? {
        return artist
    }




}