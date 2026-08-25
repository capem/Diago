package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private inline fun appIcon(
    name: String,
    autoMirror: Boolean = false,
    crossinline block: PathBuilder.() -> Unit
): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror
    ).path(
        fill = SolidColor(Color.Black),
        pathBuilder = { block() }
    ).build()
}

val Icons.Filled.Tune: ImageVector
    get() = appIcon("Filled.Tune") {
        moveTo(3f, 17f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(6f)
        verticalLineToRelative(-2f)
        horizontalLineTo(3f)
        close()
        moveTo(3f, 5f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(10f)
        verticalLineTo(5f)
        horizontalLineTo(3f)
        close()
        moveTo(13f, 21f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(-8f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(6f)
        horizontalLineTo(13f)
        close()
        moveTo(7f, 9f)
        verticalLineToRelative(2f)
        horizontalLineTo(3f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(2f)
        verticalLineTo(9f)
        horizontalLineTo(7f)
        close()
        moveTo(21f, 13f)
        verticalLineToRelative(-2f)
        horizontalLineTo(11f)
        verticalLineToRelative(2f)
        horizontalLineTo(21f)
        close()
        moveTo(17f, 9f)
        horizontalLineToRelative(2f)
        verticalLineTo(7f)
        horizontalLineToRelative(4f)
        verticalLineTo(5f)
        horizontalLineToRelative(-4f)
        verticalLineTo(3f)
        horizontalLineToRelative(-2f)
        verticalLineTo(9f)
        close()
    }

val Icons.Filled.Timer: ImageVector
    get() = appIcon("Filled.Timer") {
        moveTo(15f, 1f)
        horizontalLineTo(9f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(6f)
        verticalLineTo(1f)
        close()
        moveTo(11f, 14f)
        horizontalLineToRelative(2f)
        verticalLineTo(8f)
        horizontalLineToRelative(-2f)
        verticalLineTo(14f)
        close()
        moveTo(19.03f, 7.39f)
        lineToRelative(1.42f, -1.42f)
        curveToRelative(-0.43f, -0.51f, -0.9f, -0.99f, -1.41f, -1.41f)
        lineToRelative(-1.42f, 1.42f)
        curveTo(16.07f, 4.74f, 14.12f, 4f, 12f, 4f)
        curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
        curveToRelative(0f, 4.97f, 4.02f, 9f, 9f, 9f)
        reflectiveCurveToRelative(9f, -4.03f, 9f, -9f)
        curveTo(21f, 10.88f, 20.26f, 8.93f, 19.03f, 7.39f)
        close()
        moveTo(12f, 20f)
        curveToRelative(-3.87f, 0f, -7f, -3.13f, -7f, -7f)
        reflectiveCurveToRelative(3.13f, -7f, 7f, -7f)
        reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
        reflectiveCurveTo(15.87f, 20f, 12f, 20f)
        close()
    }

val Icons.Filled.Pause: ImageVector
    get() = appIcon("Filled.Pause") {
        moveTo(6f, 19f)
        horizontalLineToRelative(4f)
        verticalLineTo(5f)
        horizontalLineTo(6f)
        verticalLineTo(19f)
        close()
        moveTo(14f, 5f)
        verticalLineToRelative(14f)
        horizontalLineToRelative(4f)
        verticalLineTo(5f)
        horizontalLineTo(14f)
        close()
    }

val Icons.Filled.Flip: ImageVector
    get() = appIcon("Filled.Flip") {
        moveTo(15f, 21f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(-2f)
        verticalLineTo(21f)
        close()
        moveTo(19f, 9f)
        horizontalLineToRelative(2f)
        verticalLineTo(7f)
        horizontalLineToRelative(-2f)
        verticalLineTo(9f)
        close()
        moveTo(3f, 5f)
        verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(-2f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        horizontalLineToRelative(4f)
        verticalLineTo(3f)
        horizontalLineTo(5f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        close()
        moveTo(19f, 3f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(2f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(11f, 23f)
        horizontalLineToRelative(2f)
        verticalLineTo(1f)
        horizontalLineToRelative(-2f)
        verticalLineTo(23f)
        close()
        moveTo(19f, 17f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(-2f)
        verticalLineTo(17f)
        close()
        moveTo(15f, 5f)
        horizontalLineToRelative(2f)
        verticalLineTo(3f)
        horizontalLineToRelative(-2f)
        verticalLineTo(5f)
        close()
        moveTo(19f, 13f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(-2f)
        verticalLineTo(13f)
        close()
        moveTo(19f, 21f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        horizontalLineToRelative(-2f)
        verticalLineTo(21f)
        close()
    }

val Icons.AutoMirrored.Filled.VolumeMute: ImageVector
    get() = appIcon("AutoMirrored.Filled.VolumeMute", autoMirror = true) {
        moveTo(7f, 9f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(4f)
        lineToRelative(5f, 5f)
        verticalLineTo(4f)
        lineToRelative(-5f, 5f)
        horizontalLineTo(7f)
        close()
    }

val Icons.AutoMirrored.Filled.VolumeUp: ImageVector
    get() = appIcon("AutoMirrored.Filled.VolumeUp", autoMirror = true) {
        moveTo(3f, 9f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(4f)
        lineToRelative(5f, 5f)
        verticalLineTo(4f)
        lineTo(7f, 9f)
        horizontalLineTo(3f)
        close()
        moveTo(16.5f, 12f)
        curveToRelative(0f, -1.77f, -1.02f, -3.29f, -2.5f, -4.03f)
        verticalLineToRelative(8.05f)
        curveToRelative(1.48f, -0.73f, 2.5f, -2.25f, 2.5f, -4.02f)
        close()
        moveTo(14f, 3.23f)
        verticalLineToRelative(2.06f)
        curveToRelative(2.89f, 0.86f, 5f, 3.54f, 5f, 6.71f)
        reflectiveCurveToRelative(-2.11f, 5.85f, -5f, 6.71f)
        verticalLineToRelative(2.06f)
        curveToRelative(4.01f, -0.91f, 7f, -4.49f, 7f, -8.77f)
        reflectiveCurveToRelative(-2.99f, -7.86f, -7f, -8.77f)
        close()
    }

val Icons.AutoMirrored.Filled.Undo: ImageVector
    get() = appIcon("AutoMirrored.Filled.Undo", autoMirror = true) {
        moveTo(12.5f, 8f)
        curveToRelative(-2.65f, 0f, -5.05f, 0.99f, -6.9f, 2.6f)
        lineTo(2f, 7f)
        verticalLineToRelative(9f)
        horizontalLineToRelative(9f)
        lineToRelative(-3.62f, -3.62f)
        curveToRelative(1.39f, -1.16f, 3.16f, -1.88f, 5.12f, -1.88f)
        curveToRelative(3.54f, 0f, 6.55f, 2.31f, 7.6f, 5.5f)
        lineToRelative(2.37f, -0.78f)
        curveTo(21.08f, 11.03f, 17.15f, 8f, 12.5f, 8f)
        close()
    }

val Icons.Filled.KeyboardArrowUp: ImageVector
    get() = appIcon("Filled.KeyboardArrowUp") {
        moveTo(7.41f, 15.41f)
        lineTo(12f, 10.83f)
        lineToRelative(4.59f, 4.58f)
        lineTo(18f, 14f)
        lineToRelative(-6f, -6f)
        lineToRelative(-6f, 6f)
        close()
    }

val Icons.Filled.KeyboardArrowDown: ImageVector
    get() = appIcon("Filled.KeyboardArrowDown") {
        moveTo(7.41f, 8.59f)
        lineTo(12f, 13.17f)
        lineToRelative(4.59f, -4.58f)
        lineTo(18f, 10f)
        lineToRelative(-6f, 6f)
        lineToRelative(-6f, -6f)
        close()
    }

val Icons.Filled.ChevronRight: ImageVector
    get() = appIcon("Filled.ChevronRight") {
        moveTo(10f, 6f)
        lineTo(8.59f, 7.41f)
        lineTo(13.17f, 12f)
        lineToRelative(-4.58f, 4.59f)
        lineTo(10f, 18f)
        lineToRelative(6f, -6f)
        close()
    }

val Icons.Filled.EmojiEvents: ImageVector
    get() = appIcon("Filled.EmojiEvents") {
        moveTo(19f, 5f)
        horizontalLineToRelative(-2f)
        verticalLineTo(3f)
        horizontalLineTo(7f)
        verticalLineToRelative(2f)
        horizontalLineTo(5f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(1f)
        curveToRelative(0f, 2.55f, 1.92f, 4.63f, 4.39f, 4.94f)
        curveToRelative(0.63f, 1.5f, 1.98f, 2.63f, 3.61f, 2.96f)
        verticalLineTo(19f)
        horizontalLineTo(7f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(10f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(-4f)
        verticalLineToRelative(-3.1f)
        curveToRelative(1.63f, -0.33f, 2.98f, -1.46f, 3.61f, -2.96f)
        curveTo(19.08f, 12.63f, 21f, 10.55f, 21f, 8f)
        verticalLineTo(7f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(5f, 8f)
        verticalLineTo(7f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(3.82f)
        curveTo(5.84f, 10.4f, 5f, 9.3f, 5f, 8f)
        close()
        moveTo(19f, 8f)
        curveToRelative(0f, 1.3f, -0.84f, 2.4f, -2f, 2.82f)
        verticalLineTo(7f)
        horizontalLineToRelative(2f)
        verticalLineTo(8f)
        close()
    }

val Icons.Filled.AutoStories: ImageVector
    get() = appIcon("Filled.AutoStories") {
        moveTo(19f, 1f)
        lineToRelative(-5f, 5f)
        verticalLineToRelative(11f)
        lineToRelative(5f, -4.5f)
        verticalLineTo(1f)
        close()
        moveTo(1f, 6f)
        verticalLineToRelative(14.65f)
        curveToRelative(0f, 0.25f, 0.25f, 0.5f, 0.5f, 0.5f)
        curveToRelative(0.1f, 0f, 0.15f, -0.05f, 0.25f, -0.05f)
        curveTo(3.1f, 20.45f, 5.05f, 20f, 6.5f, 20f)
        curveToRelative(1.95f, 0f, 4.05f, 0.4f, 5.5f, 1.5f)
        curveToRelative(1.35f, -0.85f, 3.8f, -1.5f, 5.5f, -1.5f)
        curveToRelative(1.65f, 0f, 3.35f, 0.3f, 4.75f, 1.05f)
        curveToRelative(0.1f, 0.05f, 0.15f, 0.05f, 0.25f, 0.05f)
        curveToRelative(0.25f, 0f, 0.5f, -0.25f, 0.5f, -0.5f)
        verticalLineTo(6f)
        curveToRelative(-0.6f, -0.45f, -1.25f, -0.75f, -2f, -1f)
        verticalLineTo(19f)
        curveToRelative(-1.1f, -0.35f, -2.3f, -0.5f, -3.5f, -0.5f)
        curveToRelative(-1.7f, 0f, -4.15f, 0.65f, -5.5f, 1.5f)
        verticalLineTo(6f)
        curveToRelative(-0.8f, -0.6f, -1.85f, -1f, -3f, -1f)
        curveToRelative(-1.45f, 0f, -3.4f, 0.45f, -4.75f, 1.1f)
        verticalLineTo(19f)
        curveToRelative(-1.15f, -0.35f, -2.35f, -0.5f, -3.5f, -0.5f)
        curveToRelative(-1.2f, 0f, -2.4f, 0.15f, -3.5f, 0.5f)
        verticalLineTo(6f)
        curveToRelative(-0.6f, 0.3f, -1.15f, 0.65f, -1.75f, 1.05f)
        curveTo(1.4f, 6.75f, 1.15f, 6.4f, 1f, 6f)
        close()
    }

val Icons.Filled.People: ImageVector
    get() = appIcon("Filled.People") {
        moveTo(16f, 11f)
        curveToRelative(1.66f, 0f, 2.99f, -1.34f, 2.99f, -3f)
        reflectiveCurveTo(17.66f, 5f, 16f, 5f)
        curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
        reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
        close()
        moveTo(8f, 11f)
        curveToRelative(1.66f, 0f, 2.99f, -1.34f, 2.99f, -3f)
        reflectiveCurveTo(9.66f, 5f, 8f, 5f)
        curveTo(6.34f, 5f, 5f, 6.34f, 5f, 8f)
        reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
        close()
        moveTo(8f, 13f)
        curveToRelative(-2.33f, 0f, -7f, 1.17f, -7f, 3.5f)
        verticalLineTo(19f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(-2.5f)
        curveToRelative(0f, -2.33f, -4.67f, -3.5f, -7f, -3.5f)
        close()
        moveTo(16f, 13f)
        curveToRelative(-0.29f, 0f, -0.62f, 0.02f, -0.97f, 0.05f)
        curveToRelative(1.16f, 0.84f, 1.97f, 1.97f, 1.97f, 3.45f)
        verticalLineTo(19f)
        horizontalLineToRelative(6f)
        verticalLineToRelative(-2.5f)
        curveToRelative(0f, -2.33f, -4.67f, -3.5f, -7f, -3.5f)
        close()
    }

val Icons.Filled.SmartToy: ImageVector
    get() = appIcon("Filled.SmartToy") {
        moveTo(20f, 9f)
        verticalLineTo(7f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        horizontalLineToRelative(-3f)
        curveToRelative(0f, -1.66f, -1.34f, -3f, -3f, -3f)
        reflectiveCurveTo(9f, 3.34f, 9f, 5f)
        horizontalLineTo(6f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(2f)
        curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
        reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
        verticalLineToRelative(4f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(12f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineToRelative(-4f)
        curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
        reflectiveCurveToRelative(-1.34f, -3f, -3f, -3f)
        close()
        moveTo(18f, 19f)
        horizontalLineTo(6f)
        verticalLineTo(7f)
        horizontalLineToRelative(12f)
        verticalLineTo(19f)
        close()
        moveTo(9f, 13f)
        curveToRelative(0f, 0.83f, -0.67f, 1.5f, -1.5f, 1.5f)
        reflectiveCurveTo(6f, 13.83f, 6f, 13f)
        reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
        reflectiveCurveTo(9f, 12.17f, 9f, 13f)
        close()
        moveTo(16.5f, 14.5f)
        curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
        reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
        reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
        reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f)
        close()
    }

val Icons.Filled.RestartAlt: ImageVector
    get() = appIcon("Filled.RestartAlt") {
        moveTo(12f, 5f)
        verticalLineTo(1f)
        lineTo(7f, 6f)
        lineToRelative(5f, 5f)
        verticalLineTo(7f)
        curveToRelative(3.31f, 0f, 6f, 2.69f, 6f, 6f)
        reflectiveCurveToRelative(-2.69f, 6f, -6f, 6f)
        reflectiveCurveToRelative(-6f, -2.69f, -6f, -6f)
        horizontalLineTo(4f)
        curveToRelative(0f, 4.42f, 3.58f, 8f, 8f, 8f)
        reflectiveCurveToRelative(8f, -3.58f, 8f, -8f)
        reflectiveCurveToRelative(-3.58f, -8f, -8f, -8f)
        close()
    }

val Icons.Filled.Remove: ImageVector
    get() = appIcon("Filled.Remove") {
        moveTo(19f, 13f)
        horizontalLineTo(5f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(14f)
        verticalLineTo(13f)
        close()
    }

val Icons.AutoMirrored.Filled.ArrowBack: ImageVector
    get() = appIcon("AutoMirrored.Filled.ArrowBack", autoMirror = true) {
        moveTo(20f, 11f)
        horizontalLineTo(7.83f)
        lineToRelative(5.59f, -5.59f)
        lineTo(12f, 4f)
        lineToRelative(-8f, 8f)
        lineToRelative(8f, 8f)
        lineToRelative(1.41f, -1.41f)
        lineTo(7.83f, 13f)
        horizontalLineTo(20f)
        verticalLineToRelative(-2f)
        close()
    }

val Icons.AutoMirrored.Filled.ArrowForward: ImageVector
    get() = appIcon("AutoMirrored.Filled.ArrowForward", autoMirror = true) {
        moveTo(12f, 4f)
        lineToRelative(-1.41f, 1.41f)
        lineTo(16.17f, 11f)
        horizontalLineTo(4f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(12.17f)
        lineToRelative(-5.58f, 5.59f)
        lineTo(12f, 20f)
        lineToRelative(8f, -8f)
        close()
    }
