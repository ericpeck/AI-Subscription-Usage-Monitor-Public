package com.ericmbpeck.ai_sum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ericmbpeck.ai_sum.ui.theme.AISUMTheme
import com.ericmbpeck.ai_sum.ui.theme.Ink
import com.ericmbpeck.ai_sum.ui.theme.Neutral300
import com.ericmbpeck.ai_sum.ui.theme.Paper

@Composable
fun BroadsheetToggle(
    on: Boolean,
    onColor: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(52.dp, 44.dp)
            .clickable(role = Role.Switch, onClick = onToggle),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(Modifier.size(46.dp, 24.dp)) {
            Box(
                Modifier
                    .size(46.dp, 24.dp)
                    .background(
                        color = if (on) onColor else Neutral300,
                        shape = RoundedCornerShape(12.dp),
                    ),
            )
            Box(
                Modifier
                    .padding(start = if (on) 25.dp else 3.dp, top = 3.dp)
                    .size(18.dp)
                    .shadow(
                        elevation = 1.dp,
                        shape = CircleShape,
                        ambientColor = Ink.copy(alpha = 0.14f),
                        spotColor = Ink.copy(alpha = 0.14f),
                    )
                    .background(Paper, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F2F2)
@Composable
private fun ToggleOnPreview() {
    AISUMTheme {
        BroadsheetToggle(on = true, onColor = Color(0xFFD97757), onToggle = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F2F2)
@Composable
private fun ToggleOffPreview() {
    AISUMTheme {
        BroadsheetToggle(on = false, onColor = Color(0xFFD97757), onToggle = {})
    }
}
