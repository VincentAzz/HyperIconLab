package com.capybara.hypericonlab.modules.icon.ui.page.custom.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@Composable
fun ColorPickerDialog(
    initialColor: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val controller = rememberColorPickerController()
    var hexInput by remember { mutableStateOf(initialColor.removePrefix("#").uppercase()) }
    var isInternalUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(initialColor) {
        try {
            val color = Color(initialColor.toColorInt())
            controller.selectByColor(color, true)
            hexInput = initialColor.removePrefix("#").uppercase()
        } catch (_: Exception) {
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("选择颜色", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    controller = controller,
                )
                Spacer(modifier = Modifier.height(16.dp))
                AlphaSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp),
                    controller = controller
                )
                Spacer(modifier = Modifier.height(8.dp))
                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp),
                    controller = controller
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        val filtered = input.filter {
                            it.isDigit() || it.uppercaseChar() in 'A'..'F'
                        }.take(8).uppercase()
                        hexInput = filtered
                        if (filtered.length == 8 || filtered.length == 6) {
                            try {
                                isInternalUpdate = true
                                val fullHex = if (filtered.length == 6) "FF$filtered" else filtered
                                controller.selectByColor(
                                    Color("#$fullHex".toColorInt()),
                                    true
                                )
                            } catch (_: Exception) {
                            } finally {
                                isInternalUpdate = false
                            }
                        }
                    },
                    label = { Text("HEX 代码") },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                )

                LaunchedEffect(controller.selectedColor.value) {
                    if (!isInternalUpdate) {
                        val color = controller.selectedColor.value
                        hexInput = String.format("%08X", color.toArgb()).uppercase()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = {
                        val finalHex = if (hexInput.length == 6) "FF$hexInput" else hexInput
                        onColorSelected("#$finalHex")
                    }) { Text("选择") }
                }
            }
        }
    }
}