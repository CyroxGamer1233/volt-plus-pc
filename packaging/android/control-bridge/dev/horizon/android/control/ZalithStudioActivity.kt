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
        val model = try {
            requireNotNull(source) { "No layout was selected" }
            ZalithStudioFiles.load(source, filesDir)
        } catch (e: Exception) {
            Log.w("VoltJavaControls", "Could not open Zalith editor", e)
            Toast.makeText(this, "Could not open layout: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        var saving = false
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                ZalithStudioEditor(
                    layout = model,
                    onCancel = { if (!saving) finish() },
                    onSave = {
                        if (!saving) {
                            // pack() was evaluated on the UI thread by the editor;
                            // serialization and file I/O are serialized below.
                            saving = true
                            lifecycleScope.launch {
                                try {
                                    val saved = withContext(Dispatchers.IO) {
                                        ZalithStudioFiles.saveCopy(source!!, filesDir, model)
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
