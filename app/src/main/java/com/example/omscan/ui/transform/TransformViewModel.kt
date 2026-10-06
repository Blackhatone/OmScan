package com.example.omscan.ui.transform

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.omscan.FileManager
import java.io.File

class TransformViewModel(application: Application) : AndroidViewModel(application) {

    private val _files = MutableLiveData<List<File>>()
    val files: LiveData<List<File>> = _files

    fun loadFiles() {
        _files.value = FileManager.getAllScans(getApplication())
    }
}
