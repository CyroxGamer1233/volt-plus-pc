package dev.horizon.android.control

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Experimental host for the original Zalith editor canvas. */
class ZalithStudioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra("layout_path")
        val source = path?.let(::File)
        if (source == null) {
            Toast.makeText(this, "No layout was selected", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (!intent.getBooleanExtra("choose_layout", false)) {
            loadEditor(source)
            return
        }
        val files = try {
            ZalithStudioFiles.listLayouts(source, filesDir)
        } catch (e: Exception) {
            Log.w("VoltJavaControls", "Could not list Zalith layouts", e)
            Toast.makeText(this, "Could not open layouts: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (files.isEmpty()) { finish(); return }
        val labels = files.map { file ->
            val name = file.name.removeSuffix(".json")
            if (name.matches(Regex("[a-f0-9]{64}"))) "Layout ${name.take(12)}" else name.take(60)
        }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("Choose a Zalith layout to edit")
            .setItems(labels) { _, index ->
                loadEditor(files[index])
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun loadEditor(source: File) {
        try {
            openEditor(source, ZalithStudioFiles.load(source, filesDir))
        } catch (e: Exception) {
            Log.w("VoltJavaControls", "Could not open selected layout", e)
            Toast.makeText(this, "Could not open layout: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun openEditor(source: File, model: com.movtery.layer_controller.observable.ObservableControlLayout) {
        var saving = false
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                ZalithStudioEditor(
                    layout = model,
                    onCancel = { if (!saving) finish() },
                    onSave = { packed ->
                        if (!saving) {
                            // pack() was evaluated on the UI thread by the editor;
                            // serialization and file I/O are serialized below.
                            saving = true
                            lifecycleScope.launch {
                                try {
                                    val saved = withContext(Dispatchers.IO) {
                                        ZalithStudioFiles.saveCopy(source, filesDir, packed)
                                    }
                                    setResult(Activity.RESULT_OK, Intent().putExtra("layout_path", saved.path))
                                    Toast.makeText(this@ZalithStudioActivity, "Saved a new layout copy", Toast.LENGTH_SHORT).show()
                                    finish()
                                } catch (e: Exception) {
                                    saving = false
                                    Log.w("VoltJavaControls", "Could not save Zalith layout", e)
                                    Toast.makeText(this@ZalithStudioActivity, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}
