package com.example.omscan.ui.transform

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.omscan.FileManager
import com.example.omscan.R
import com.example.omscan.TextRecognizer
import com.example.omscan.databinding.FragmentTransformBinding
import com.example.omscan.databinding.ItemTransformBinding
import java.io.File
import java.util.ArrayList

class TransformFragment : Fragment() {

    private var _binding: FragmentTransformBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TransformViewModel
    private lateinit var adapter: TransformAdapter

    private var isSelectionMode = false
    private val selectedFiles = mutableSetOf<File>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(TransformViewModel::class.java)
        _binding = FragmentTransformBinding.inflate(inflater, container, false)
        
        adapter = TransformAdapter(
            onClick = { file -> 
                if (isSelectionMode) {
                    toggleSelection(file)
                } else {
                    openFile(file)
                }
            },
            onLongClick = { file ->
                if (!isSelectionMode) {
                    enterSelectionMode(file)
                }
            },
            onMoreClick = { file, view -> showPopupMenu(file, view) },
            isSelected = { file -> selectedFiles.contains(file) }
        )
        binding.recyclerviewTransform.adapter = adapter
        
        viewModel.files.observe(viewLifecycleOwner) { files ->
            adapter.submitList(files)
            binding.emptyState?.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        }

        setupSelectionToolbar()
        
        return binding.root
    }

    private fun setupSelectionToolbar() {
        binding.toolbarSelection?.setNavigationOnClickListener {
            exitSelectionMode()
        }
        binding.toolbarSelection?.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> shareMultipleFiles(selectedFiles.toList())
                R.id.action_delete -> deleteMultipleFiles(selectedFiles.toList())
            }
            true
        }
    }

    private fun enterSelectionMode(file: File) {
        isSelectionMode = true
        selectedFiles.add(file)
        binding.toolbarSelection?.visibility = View.VISIBLE
        updateSelectionTitle()
        adapter.notifyDataSetChanged()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedFiles.clear()
        binding.toolbarSelection?.visibility = View.GONE
        adapter.notifyDataSetChanged()
    }

    private fun toggleSelection(file: File) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file)
        } else {
            selectedFiles.add(file)
        }
        
        if (selectedFiles.isEmpty()) {
            exitSelectionMode()
        } else {
            updateSelectionTitle()
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateSelectionTitle() {
        binding.toolbarSelection?.title = "${selectedFiles.size} seleccionados"
    }

    private fun shareMultipleFiles(files: List<File>) {
        if (files.isEmpty()) return
        
        val uris = ArrayList(files.map { 
            FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", it)
        })
        
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/pdf"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir documentos"))
        exitSelectionMode()
    }

    private fun deleteMultipleFiles(files: List<File>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar documentos")
            .setMessage("¿Estás seguro de que quieres eliminar ${files.size} documentos?")
            .setPositiveButton("Eliminar") { _, _ ->
                files.forEach { FileManager.deleteFile(it) }
                exitSelectionMode()
                viewModel.loadFiles()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFiles()
    }

    private fun openFile(file: File) {
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (file.extension == "pdf") "application/pdf" else "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Abrir con"))
    }

    private fun showPopupMenu(file: File, view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add("Compartir")
        if (file.extension != "pdf") {
            popup.menu.add("Extraer Texto (OCR)")
        }
        popup.menu.add("Renombrar")
        popup.menu.add("Eliminar")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Compartir" -> shareFile(file)
                "Extraer Texto (OCR)" -> extractText(file)
                "Renombrar" -> showRenameDialog(file)
                "Eliminar" -> deleteFile(file)
            }
            true
        }
        popup.show()
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension == "pdf") "application/pdf" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir vía"))
    }

    private fun extractText(file: File) {
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        Toast.makeText(requireContext(), "Procesando texto...", Toast.LENGTH_SHORT).show()
        TextRecognizer.extractText(requireContext(), uri, 
            onSuccess = { text ->
                if (text.isBlank()) {
                    Toast.makeText(requireContext(), "No se encontró texto", Toast.LENGTH_SHORT).show()
                } else {
                    showTextDialog(text)
                }
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), "Error OCR: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showTextDialog(text: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Texto Extraído")
            .setMessage(text)
            .setPositiveButton("Copiar") { _, _ ->
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = android.content.ClipData.newPlainText("Texto Extraído", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showRenameDialog(file: File) {
        val editText = EditText(requireContext())
        editText.setText(file.nameWithoutExtension)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Renombrar Archivo")
            .setView(editText)
            .setPositiveButton("Renombrar") { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank()) {
                    FileManager.renameFile(file, newName)
                    viewModel.loadFiles()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteFile(file: File) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Archivo")
            .setMessage("¿Estás seguro de que quieres eliminar ${file.name}?")
            .setPositiveButton("Eliminar") { _, _ ->
                FileManager.deleteFile(file)
                viewModel.loadFiles()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class TransformAdapter(
        private val onClick: (File) -> Unit,
        private val onLongClick: (File) -> Unit,
        private val onMoreClick: (File, View) -> Unit,
        private val isSelected: (File) -> Boolean
    ) : ListAdapter<File, TransformViewHolder>(object : DiffUtil.ItemCallback<File>() {
            override fun areItemsTheSame(oldItem: File, newItem: File): Boolean =
                oldItem.absolutePath == newItem.absolutePath

            override fun areContentsTheSame(oldItem: File, newItem: File): Boolean =
                oldItem.lastModified() == newItem.lastModified()
        }) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransformViewHolder {
            val binding = ItemTransformBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return TransformViewHolder(binding, onClick, onLongClick, onMoreClick, isSelected)
        }

        override fun onBindViewHolder(holder: TransformViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    class TransformViewHolder(
        private val binding: ItemTransformBinding,
        private val onClick: (File) -> Unit,
        private val onLongClick: (File) -> Unit,
        private val onMoreClick: (File, View) -> Unit,
        private val isSelected: (File) -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File) {
            binding.textViewItemTransform.text = file.name
            
            if (file.extension == "pdf") {
                binding.imageViewItemTransform.setImageResource(R.drawable.ic_slideshow_black_24dp)
            } else {
                Glide.with(binding.imageViewItemTransform)
                    .load(file)
                    .centerCrop()
                    .into(binding.imageViewItemTransform)
            }
            
            // Highlight if selected
            val context = binding.root.context
            if (isSelected(file)) {
                binding.root.setCardBackgroundColor(context.getColor(R.color.primary_container))
            } else {
                binding.root.setCardBackgroundColor(context.getColor(R.color.background))
            }

            binding.root.setOnClickListener { onClick(file) }
            binding.root.setOnLongClickListener { 
                onLongClick(file)
                true
            }
            binding.buttonMore.setOnClickListener { onMoreClick(file, it) }
        }
    }
}
