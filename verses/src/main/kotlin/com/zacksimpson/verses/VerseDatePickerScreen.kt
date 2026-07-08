package com.zacksimpson.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.designVerticalPxToSp
import com.thelightphone.sdk.ui.gridUnitsAsDp
import androidx.compose.runtime.collectAsState
import java.time.LocalDate
import java.time.YearMonth

private const val DAY_NUMBER_DESIGN_PX = 30f

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
private val DAY_HEADERS = listOf("S", "M", "T", "W", "T", "F", "S")

/**
 * Calendar month picker for browsing past verses. Always opens on the current month.
 * Unlike reminders-tool's DatePickerScreen, this never allows navigating into the
 * future: the forward chevron is a no-op once viewing the current month, and any day
 * after today within the current month is rendered blank (same treatment as the
 * leading before-the-1st cells) rather than shown as a disabled/grayed-out number.
 */
class VerseDatePickerScreen(
    sealedActivity: SealedLightActivity,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val today = remember { LocalDate.now() }
        var viewYear by remember { mutableIntStateOf(today.year) }
        var viewMonth by remember { mutableIntStateOf(today.monthValue) } // 1-12

        fun prevMonth() {
            if (viewMonth == 1) {
                viewMonth = 12
                viewYear -= 1
            } else {
                viewMonth -= 1
            }
        }

        fun nextMonth() {
            if (viewYear == today.year && viewMonth == today.monthValue) return
            if (viewMonth == 12) {
                viewMonth = 1
                viewYear += 1
            } else {
                viewMonth += 1
            }
        }

        val firstDayOfWeek = LocalDate.of(viewYear, viewMonth, 1).dayOfWeek.value % 7 // Sun=0..Sat=6
        val daysInMonth = YearMonth.of(viewYear, viewMonth).lengthOfMonth()
        val isCurrentMonth = viewYear == today.year && viewMonth == today.monthValue
        val cells = buildList {
            repeat(firstDayOfWeek) { add(null) }
            for (d in 1..daysInMonth) {
                val isFuture = isCurrentMonth && d > today.dayOfMonth
                add(if (isFuture) null else d)
            }
        }
        val rows = cells.chunked(7).map { row -> (row + List(7) { null }).take(7) }

        LightTheme(colors = themeColors) {
            SwipeBackContainer(onSwipeBack = { goBack(Unit) }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 0.65f.gridUnitsAsDp(),
                                bottom = 1f.gridUnitsAsDp(),
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightIcon(
                            icon = LightIcons.BACK,
                            size = 2f,
                            modifier = Modifier.clickable { prevMonth() },
                        )
                        LightText(text = "${MONTH_NAMES[viewMonth - 1]} $viewYear", variant = LightTextVariant.Paragraph)
                        LightIcon(
                            icon = LightIcons.ARROW_RIGHT,
                            size = 2f,
                            modifier = Modifier.clickable { nextMonth() },
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        DAY_HEADERS.forEach { d ->
                            Box(
                                modifier = Modifier.weight(1f).padding(vertical = 0.5f.gridUnitsAsDp()),
                                contentAlignment = Alignment.Center,
                            ) {
                                DayHeaderText(d)
                            }
                        }
                    }

                    Column {
                        rows.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                row.forEach { day ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(vertical = 0.55f.gridUnitsAsDp())
                                            .let {
                                                if (day != null) {
                                                    it.clickable {
                                                        val dateStr = "%04d-%02d-%02d".format(viewYear, viewMonth, day)
                                                        navigateTo(screenFactory = { VerseForDateScreen(it, dateStr) })
                                                    }
                                                } else {
                                                    it
                                                }
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (day != null) {
                                            val dateStr = "%04d-%02d-%02d".format(viewYear, viewMonth, day)
                                            val showUnderline = dateStr == today.toString()
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                LightText(text = day.toString(), variant = LightTextVariant.Copy)
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 0.2f.gridUnitsAsDp())
                                                        .width(14.dp)
                                                        .height(2.dp)
                                                        .background(
                                                            if (showUnderline) LightThemeTokens.colors.content else Color.Transparent,
                                                        ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                LightBottomBar(
                    items = listOf(LightBarButton.LightIcon(LightIcons.CLOSE, onClick = { goBack(Unit) })),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            }
        }
    }
}

/** Bold day-of-week header letter — LightTextVariant has no weight override, so this
 *  goes straight to Text at the same 30sp size the day-number cells use. */
@Composable
private fun DayHeaderText(text: String) {
    Text(
        text = text,
        style = TextStyle(
            color = LightThemeTokens.colors.content,
            fontFamily = LightThemeTokens.typography.copy.fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = DAY_NUMBER_DESIGN_PX.designVerticalPxToSp(),
        ),
    )
}
