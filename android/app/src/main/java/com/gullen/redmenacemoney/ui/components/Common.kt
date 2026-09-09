package com.gullen.redmenacemoney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gullen.redmenacemoney.ui.theme.Ink
import com.gullen.redmenacemoney.ui.theme.LineGrey
import com.gullen.redmenacemoney.ui.theme.MonoFamily
import com.gullen.redmenacemoney.ui.theme.PaperCard
import com.gullen.redmenacemoney.ui.theme.Pine
import com.gullen.redmenacemoney.ui.theme.Rail
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private val cadFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "CA")).apply {
    currency = Currency.getInstance("CAD")
    maximumFractionDigits = 0
}
private val cadFormat2: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "CA")).apply {
    currency = Currency.getInstance("CAD")
    maximumFractionDigits = 2
}

fun money(n: Double): String = cadFormat.format(n)
fun money2(n: Double): String = cadFormat2.format(n)

/**
 * A flat, hairline-bordered card with a colored left edge — the "ledger card" look used
 * throughout the web app. content is a normal ColumnScope, same as calling Column {} directly.
 */
@Composable
fun SectionCard(
    accentColor: Color = Rail,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, LineGrey, RoundedCornerShape(3.dp))
            .background(PaperCard)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accentColor)
        )
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun MoneyText(amount: Double, color: Color = Ink, fontSize: TextUnit = 16.sp) {
    Text(text = money(amount), color = color, fontFamily = MonoFamily, fontSize = fontSize, fontWeight = FontWeight.SemiBold)
}

/** The "rail line with a marker sliding toward a flag" goal progress visual from the web app. */
@Composable
fun GoalTrack(progress: Float, modifier: Modifier = Modifier) {
    val clamped = progress.coerceIn(0f, 1f)
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(28.dp)) {
        val trackWidth = maxWidth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(3.dp))
                .background(LineGrey)
        )
        Box(
            modifier = Modifier
                .width(trackWidth * clamped)
                .height(6.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(3.dp))
                .background(Pine)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = (trackWidth * clamped - 12.dp).coerceAtLeast(0.dp))
        ) {
            Text("\uD83D\uDE82")
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            Text("\uD83C\uDFC1")
        }
    }
}

// ---------------------------------------------------------------
// Form helpers used across every editable screen
// ---------------------------------------------------------------

@Composable
fun LabeledMoneyField(
    label: String,
    value: Double,
    modifier: Modifier = Modifier,
    onValueChange: (Double) -> Unit
) {
    var text by androidx.compose.runtime.remember(value) {
        androidx.compose.runtime.mutableStateOf(if (value == 0.0) "" else formatPlain(value))
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
        androidx.compose.material3.OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                it.toDoubleOrNull()?.let(onValueChange)
            },
            modifier = Modifier.width(120.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = MonoFamily, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
        )
    }
}

@Composable
fun LabeledTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(160.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
        )
    }
}

private fun formatPlain(n: Double): String =
    if (n == n.toLong().toDouble()) n.toLong().toString() else n.toString()
