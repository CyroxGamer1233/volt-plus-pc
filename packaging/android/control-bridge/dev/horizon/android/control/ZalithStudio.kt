package dev.horizon.android.control

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.movtery.layer_controller.ControlEditorLayer
import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.data.DefaultSize
import com.movtery.layer_controller.data.NormalData
import com.movtery.layer_controller.data.VisibilityType
import com.movtery.layer_controller.data.lang.createTranslatable
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.layout.createNewLayer
import com.movtery.layer_controller.observable.ObservableControlLayer
import com.movtery.layer_controller.observable.ObservableControlLayout
import com.movtery.layer_controller.observable.ObservableJoystickData
import com.movtery.layer_controller.observable.ObservableNormalData
import com.movtery.layer_controller.observable.ObservableTextData
import com.movtery.layer_controller.observable.ObservableWidget
import com.movtery.layer_controller.utils.snap.SnapMode
import java.util.UUID

/** Editor operations act on the original Zalith observable model. */
object ZalithStudioActions {
    fun addButton(layer: ObservableControlLayer): ObservableNormalData {
        val button = NormalData(
            text = createTranslatable("Button"),
            uuid = UUID.randomUUID().toString().replace("-", ""),
            position = ButtonPosition(5000, 5000),
            buttonSize = DefaultSize,
            visibilityType = VisibilityType.ALWAYS,
            _clickEvents = emptyList(),
            isSwipple = false,
            isPenetrable = false,
            isToggleable = false,
        )
        return ObservableNormalData(button).also { layer.addNormalButton(it) }
    }

    fun delete(layer: ObservableControlLayer, widget: ObservableWidget) {
        when (widget) {
            is ObservableNormalData -> layer.removeNormalButton(widget.uuid)
            is ObservableTextData -> layer.removeTextBox(widget.uuid)
            is ObservableJoystickData -> layer.removeJoystickButton(widget.uuid)
        }
    }
}

/**
 * Real upstream layout editor canvas with a small host toolbar. The caller
 * owns loading, save/rollback and navigation. This component is not a full
 * replacement for Zalith's application editor or a gameplay overlay.
 */
@Composable
fun ZalithStudioEditor(
    layout: ObservableControlLayout,
    onSave: (ControlLayout) -> Unit,
    onCancel: () -> Unit,
) {
    val layers by layout.layers.collectAsState()
    var focusedLayer by remember(layout) { mutableStateOf<ObservableControlLayer?>(null) }
    var selected by remember(layout) { mutableStateOf<ObservableWidget?>(null) }
    var selectedLayer by remember(layout) { mutableStateOf<ObservableControlLayer?>(null) }
    var keyName by remember(selected) { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(Color(0xff17151e))) {
        ControlEditorLayer(
            observedLayout = layout,
            selectedWidget = selected,
            onButtonTap = { widget, layer -> selected = widget; selectedLayer = layer },
            onBackgroundClick = { selected = null; selectedLayer = null },
            floatingButtons = {},
            enableSnap = true,
            snapInAllLayers = true,
            snapMode = SnapMode.Local,
            focusedLayer = focusedLayer,
            isDark = true,
        )
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Color(0xe6221f2c))) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onCancel) { Text("Cancel") }
                Button(onClick = { onSave(layout.pack()) }) { Text("Save copy") }
                OutlinedButton(onClick = {
                    val layer = focusedLayer ?: layers.firstOrNull() ?: layout.addLayer(createNewLayer("Controls"))
                    layer.editorHide = false
                    selectedLayer = layer
                    selected = ZalithStudioActions.addButton(layer)
                }) { Text("Add button") }
                OutlinedButton(onClick = { focusedLayer = null; selected = null; selectedLayer = null }) { Text("All layers") }
                layers.forEach { layer ->
                    OutlinedButton(onClick = {
                        layer.editorHide = false
                        focusedLayer = layer
                        selected = null
                        selectedLayer = null
                    }) { Text(layer.name.ifBlank { "Layer" }) }
                }
            }
        }
        val normal = selected as? ObservableNormalData
        if (normal != null) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xe6221f2c)).padding(6.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = normal.text.default, onValueChange = { normal.text.default = it.take(128) }, label = { Text("Label") }, singleLine = true, modifier = Modifier.width(150.dp))
                    FilterChip(selected = normal.isToggleable, onClick = { normal.isToggleable = !normal.isToggleable }, label = { Text("Toggle") })
                    FilterChip(selected = normal.isSwipple, onClick = { normal.isSwipple = !normal.isSwipple }, label = { Text("Swipe") })
                    FilterChip(selected = normal.isPenetrable, onClick = { normal.isPenetrable = !normal.isPenetrable }, label = { Text("Passthrough") })
                    OutlinedButton(onClick = { selectedLayer?.let { ZalithStudioActions.delete(it, normal) }; selected = null; selectedLayer = null }) { Text("Delete") }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = keyName, onValueChange = { keyName = it.take(64); error = null }, label = { Text("GLFW key name") }, singleLine = true, modifier = Modifier.width(190.dp))
                    OutlinedButton(onClick = {
                        val key = keyName.trim()
                        if (!key.matches(Regex("GLFW_KEY_[A-Z0-9_]+"))) {
                            error = "Use a GLFW key name, e.g. GLFW_KEY_W"
                        } else {
                            normal.addEvent(ClickEvent(ClickEvent.Type.Key, key))
                            keyName = ""
                        }
                    }) { Text("Add key") }
                    normal.clickEvents.forEach { event ->
                        AssistChip(onClick = { normal.removeEvent(event) }, label = { Text("${event.key} ×") })
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        } else if (selected != null) {
            Row(Modifier.align(Alignment.BottomCenter).background(Color(0xe6221f2c)).padding(6.dp)) {
                Text("Drag or resize the selected control", Modifier.align(Alignment.CenterVertically))
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { selectedLayer?.let { layer -> selected?.let { ZalithStudioActions.delete(layer, it) } }; selected = null; selectedLayer = null }) { Text("Delete") }
            }
        }
    }
}
