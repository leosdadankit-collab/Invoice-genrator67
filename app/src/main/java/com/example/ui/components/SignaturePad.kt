package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    onSignatureSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val paths = remember { mutableStateListOf<List<Offset>>() }
    val currentPath = remember { mutableStateListOf<Offset>() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Draw Your Digital Signature",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .pointerInteropFilter { motionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            currentPath.clear()
                            currentPath.add(Offset(motionEvent.x, motionEvent.y))
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            currentPath.add(Offset(motionEvent.x, motionEvent.y))
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (currentPath.isNotEmpty()) {
                                paths.add(currentPath.toList())
                                currentPath.clear()
                            }
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // Draw existing saved paths
                paths.forEach { points ->
                    if (points.size > 1) {
                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF0F2942),
                            style = Stroke(
                                width = 4f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Draw active stroke
                if (currentPath.size > 1) {
                    val activePath = Path().apply {
                        moveTo(currentPath[0].x, currentPath[0].y)
                        for (i in 1 until currentPath.size) {
                            lineTo(currentPath[i].x, currentPath[i].y)
                        }
                    }
                    drawPath(
                        path = activePath,
                        color = Color(0xFF0F2942),
                        style = Stroke(
                            width = 4f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            if (paths.isEmpty() && currentPath.isEmpty()) {
                Text(
                    text = "Sign here with finger / stylus",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = {
                    paths.clear()
                    currentPath.clear()
                }
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    if (paths.isNotEmpty()) {
                        val filePath = saveSignatureBitmap(context, paths.toList())
                        if (filePath != null) {
                            onSignatureSaved(filePath)
                        }
                    }
                },
                enabled = paths.isNotEmpty()
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Signature")
            }
        }
    }
}

private fun saveSignatureBitmap(context: Context, strokePaths: List<List<Offset>>): String? {
    return try {
        val width = 400
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint().apply {
            color = android.graphics.Color.rgb(15, 41, 66)
            strokeWidth = 5f
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (points in strokePaths) {
            if (points.size > 1) {
                val path = android.graphics.Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                canvas.drawPath(path, paint)
            }
        }

        val file = File(context.filesDir, "signature_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
