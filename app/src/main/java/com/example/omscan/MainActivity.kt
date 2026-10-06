package com.example.omscan

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import com.example.omscan.databinding.ActivityMainBinding
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.net.Uri

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var isIdCardMode = false
    private var frontImageUri: Uri? = null

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.let { handleScanResult(it) }
        } else {
            // Reset ID card mode if scan was cancelled
            isIdCardMode = false
            frontImageUri = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedTheme = sharedPref.getInt("theme", AppCompatDelegate.MODE_NIGHT_NO)
        AppCompatDelegate.setDefaultNightMode(savedTheme)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab?.setOnClickListener {
            showScanModeDialog()
        }

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        val navController = navHostFragment.navController

        binding.navView?.let {
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_home
                ),
                binding.drawerLayout
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            it.setupWithNavController(navController)
            
            // Handle custom Info item
            it.setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_info -> {
                        Toast.makeText(this, R.string.developed_by, Toast.LENGTH_LONG).show()
                        binding.drawerLayout?.closeDrawers()
                        true
                    }
                    else -> {
                        val handled = androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
                        if (handled) binding.drawerLayout?.closeDrawers()
                        handled
                    }
                }
            }
        }

        binding.appBarMain.contentMain.bottomNavView?.let {
            it.setupWithNavController(navController)
            it.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_info -> {
                        Toast.makeText(this, R.string.developed_by, Toast.LENGTH_LONG).show()
                        true
                    }
                    else -> androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        // Using findViewById because NavigationView exists in different layout files
        // between w600dp and w1240dp
        val navView: NavigationView? = findViewById(R.id.nav_view)
        if (navView == null) {
            // The navigation drawer already has the items including the items in the overflow menu
            // We only inflate the overflow menu if the navigation drawer isn't visible
            menuInflater.inflate(R.menu.overflow, menu)
        }
        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_settings)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun showScanModeDialog() {
        val options = arrayOf(getString(R.string.mode_document), getString(R.string.mode_id_card))
        AlertDialog.Builder(this)
            .setTitle(R.string.title_scan_mode)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        isIdCardMode = false
                        startScan()
                    }
                    1 -> {
                        isIdCardMode = true
                        frontImageUri = null
                        Toast.makeText(this, R.string.msg_scan_front, Toast.LENGTH_LONG).show()
                        startScan(limit = 1)
                    }
                }
            }
            .show()
    }

    private fun startScan(limit: Int = 100) {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(limit)
            .setResultFormats(RESULT_FORMAT_JPEG) // We generate our own PDF
            .setScannerMode(SCANNER_MODE_FULL)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)
        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                Log.e("OmScan", "Error starting scan", e)
                Toast.makeText(this, "Error starting scan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleScanResult(result: GmsDocumentScanningResult) {
        if (isIdCardMode) {
            if (frontImageUri == null) {
                // Front scanned, now scan back
                frontImageUri = result.pages?.get(0)?.imageUri
                Toast.makeText(this, R.string.msg_scan_back, Toast.LENGTH_LONG).show()
                startScan(limit = 1)
            } else {
                // Back scanned, now generate ID card PDF
                val backImageUri = result.pages?.get(0)?.imageUri
                if (backImageUri != null) {
                    showNameDialog(listOf(frontImageUri!!, backImageUri), isIdCard = true)
                }
                isIdCardMode = false
                frontImageUri = null
            }
        } else {
            // Normal document mode
            val imageUris = result.pages?.map { it.imageUri } ?: emptyList()
            if (imageUris.isNotEmpty()) {
                showNameDialog(imageUris, isIdCard = false)
            }
        }
    }

    private fun showNameDialog(imageUris: List<Uri>, isIdCard: Boolean) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_file_name, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_text_file_name)
        
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_name_title)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val fileName = editText?.text?.toString()?.ifBlank { "Scan_${System.currentTimeMillis()}" } ?: "Scan_${System.currentTimeMillis()}"
                generatePdf(imageUris, fileName, isIdCard)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun generatePdf(imageUris: List<Uri>, fileName: String, isIdCard: Boolean) {
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val quality = sharedPref.getInt("pdf_quality", 50)
        
        Toast.makeText(this, R.string.msg_generating_pdf, Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch(Dispatchers.IO) {
            val pdfFile = if (isIdCard) {
                PdfGenerator.generateIdCardPdf(this@MainActivity, imageUris[0], imageUris[1], fileName, quality)
            } else {
                PdfGenerator.generateCompressedPdf(this@MainActivity, imageUris, fileName, quality)
            }
            
            withContext(Dispatchers.Main) {
                if (pdfFile != null) {
                    Toast.makeText(this@MainActivity, getString(R.string.msg_pdf_ready, pdfFile.name), Toast.LENGTH_LONG).show()
                    // Refresh fragment list if visible
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_home)
                } else {
                    Toast.makeText(this@MainActivity, "Error al generar PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
