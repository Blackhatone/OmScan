package com.example.omscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    /**
     * Genera un PDF comprimido a partir de una lista de imágenes.
     * @param quality Calidad de compresión de 0 a 100.
     */
    fun generateCompressedPdf(
        context: Context,
        imageUris: List<Uri>,
        fileName: String,
        quality: Int
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()

        imageUris.forEachIndexed { index, uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@forEachIndexed
                
                // Redimensionar para ahorrar espacio si es necesario
                val scaledBitmap = scaleBitmap(originalBitmap, 1200f) // Max width/height 1200px
                
                val pageInfo = PdfDocument.PageInfo.Builder(scaledBitmap.width, scaledBitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                
                val canvas = page.canvas
                canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)
                
                pdfDocument.finishPage(page)
                
                if (scaledBitmap != originalBitmap) scaledBitmap.recycle()
                originalBitmap.recycle()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val scansDir = FileManager.getScansDirectory(context)
        val pdfFile = File(scansDir, if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf")

        return try {
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Genera un PDF con el frente y dorso de una cédula en una sola página.
     */
    fun generateIdCardPdf(
        context: Context,
        frontUri: Uri,
        backUri: Uri,
        fileName: String,
        quality: Int
    ): File? {
        val pdfDocument = PdfDocument()
        
        // Tamaño A4 estándar a 72 DPI (aprox. 595x842)
        val pageWidth = 595
        val pageHeight = 842
        
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        try {
            val frontBitmap = loadAndScale(context, frontUri, 400f)
            val backBitmap = loadAndScale(context, backUri, 400f)

            if (frontBitmap != null && backBitmap != null) {
                // Dibujar frente (arriba)
                val centerX = (pageWidth - frontBitmap.width) / 2f
                canvas.drawBitmap(frontBitmap, centerX, 100f, paint)

                // Dibujar dorso (abajo del frente)
                canvas.drawBitmap(backBitmap, centerX, 100f + frontBitmap.height + 50f, paint)
                
                frontBitmap.recycle()
                backBitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        pdfDocument.finishPage(page)

        val scansDir = FileManager.getScansDirectory(context)
        val pdfFile = File(scansDir, if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf")

        return try {
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadAndScale(context: Context, uri: Uri, maxSize: Float): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(inputStream) ?: return null
        return scaleBitmap(original, maxSize)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scale = maxSize / Math.max(width, height)
        if (scale >= 1.0f) return bitmap

        val matrix = Matrix()
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }
}
