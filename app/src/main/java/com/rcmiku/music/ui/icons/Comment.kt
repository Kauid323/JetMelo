package com.rcmiku.music.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Comment: ImageVector
    get() {
        if (_Comment != null) {
            return _Comment!!
        }
        _Comment = ImageVector.Builder(
            name = "Comment",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 4.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(21f, 11.5f)
                curveTo(21.003f, 8.865f, 19.963f, 6.338f, 18.11f, 4.475f)
                curveTo(16.256f, 2.61f, 13.74f, 1.563f, 11.115f, 1.563f)
                curveTo(8.49f, 1.563f, 5.974f, 2.61f, 4.12f, 4.475f)
                curveTo(2.267f, 6.338f, 1.227f, 8.865f, 1.23f, 11.5f)
                curveTo(1.227f, 14.135f, 2.267f, 16.662f, 4.12f, 18.525f)
                curveTo(5.974f, 20.39f, 8.49f, 21.437f, 11.115f, 21.437f)
                curveTo(12.3f, 21.437f, 13.475f, 21.196f, 14.57f, 20.735f)
                lineTo(21f, 22.5f)
                lineTo(19.23f, 16.083f)
                curveTo(20.387f, 14.717f, 21.006f, 13.12f, 21f, 11.5f)
                close()
            }
        }.build()
        return _Comment!!
    }

private var _Comment: ImageVector? = null
