package com.example.omscan

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object TextRecognizer {
    fun extractText(context: Context, imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    onSuccess(visionText.text)
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
