package dev.heckr.kitsudo.presentation.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.heckr.kitsudo.R
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

/** Visual constants for the scroll wheels. Must be odd so a row sits dead-center. */
private const val WHEEL_VISIBLE_ROWS = 5
private val WheelRowHeight = 40.dp

/**
 * Cyclic wheels (hour/minute) render their values repeated this many times and
 * start in the middle block, so scrolling feels endless in both directions.
 * Odd so there's a true middle block.
 */
private const val WHEEL_REPEAT = 201

/** How far back the date wheel scrolls (lets you back-date a deadline a little). */
private const val DATE_PAST_DAYS = 7L
private const val DATE_FUTURE_DAYS = 365L * 2

/**
 * Deadline picker: a bottom sheet with one-tap relative presets above three
 * snap-scrolling wheels (date · hour · minute). Fully custom - no Material
 * date/time dialogs - so it matches the app's look and sets common deadlines
 * in a single tap.
 *
 * - [showClearOption] shows a "Clear" button to remove the deadline.
 * - [onClear] is called when that button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeadlinePicker(
    initialDeadlineAt: Long? = null,
    onDeadlinePicked: (Long) -> Unit,
    onDismiss: () -> Unit,
    showClearOption: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now() }
    val startDate = remember { today.minusDays(DATE_PAST_DAYS) }
    val dateCount = remember { (DATE_PAST_DAYS + DATE_FUTURE_DAYS + 1).toInt() }

    val initial = remember(initialDeadlineAt) {
        initialDeadlineAt
            ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }
            ?: LocalDateTime.now().plusHours(1).withMinute(0)
    }

    fun dateIndexOf(date: LocalDate): Int =
        ChronoUnit.DAYS.between(startDate, date).toInt().coerceIn(0, dateCount - 1)

    val dateState = rememberLazyListState(dateIndexOf(initial.toLocalDate()))
    // Cyclic wheels start in the middle block so they scroll endlessly both ways.
    val hourState = rememberLazyListState(WHEEL_REPEAT / 2 * 24 + initial.hour)
    val minuteState = rememberLazyListState(WHEEL_REPEAT / 2 * 60 + initial.minute)

    var dateIndex by remember { mutableIntStateOf(dateIndexOf(initial.toLocalDate())) }
    var hour by remember { mutableIntStateOf(initial.hour) }
    var minute by remember { mutableIntStateOf(initial.minute) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val todayLabel = stringResource(R.string.deadline_relative_today)
    val tomorrowLabel = stringResource(R.string.deadline_relative_tomorrow)

    fun selectedMillis(): Long {
        val date = startDate.plusDays(dateIndex.toLong())
        return LocalDateTime.of(date, LocalTime.of(hour, minute))
            .atZone(zone).toInstant().toEpochMilli()
    }

    /** Animates all three wheels to a preset moment (cyclic wheels pick the nearest match). */
    fun applyPreset(target: LocalDateTime) {
        scope.launch { dateState.animateScrollToItem(dateIndexOf(target.toLocalDate())) }
        scope.launch {
            hourState.animateScrollToItem(
                nearestCyclicIndex(hourState.firstVisibleItemIndex, 24, target.hour),
            )
        }
        scope.launch {
            minuteState.animateScrollToItem(
                nearestCyclicIndex(minuteState.firstVisibleItemIndex, 60, target.minute),
            )
        }
    }

    /** Hides the sheet gracefully, then runs [after]. */
    fun dismissThen(after: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) after()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            // -- Header --------------------------------------------------
            Text(
                text = stringResource(R.string.deadline_picker_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )

            // -- Presets -------------------------------------------------
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                presets(today).forEach { preset ->
                    SuggestionChip(
                        onClick = { applyPreset(preset.moment) },
                        label = { Text(stringResource(preset.labelRes)) },
                    )
                }
            }

            // -- Wheels --------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .height(WheelRowHeight * WHEEL_VISIBLE_ROWS),
                contentAlignment = Alignment.Center,
            ) {
                // Center selection highlight, behind the wheels.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WheelRowHeight)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    WheelColumn(
                        state = dateState,
                        valueCount = dateCount,
                        cyclic = false,
                        onCentered = { dateIndex = it },
                        label = {
                            dateLabel(startDate.plusDays(it.toLong()), today, todayLabel, tomorrowLabel)
                        },
                        modifier = Modifier.weight(2.4f),
                    )
                    WheelColumn(
                        state = hourState,
                        valueCount = 24,
                        cyclic = true,
                        onCentered = { hour = it },
                        label = { "%02d".format(it) },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WheelColumn(
                        state = minuteState,
                        valueCount = 60,
                        cyclic = true,
                        onCentered = { minute = it },
                        label = { "%02d".format(it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // -- Actions -------------------------------------------------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                if (showClearOption && onClear != null) {
                    TextButton(onClick = { dismissThen(onClear) }) {
                        Text(stringResource(R.string.task_deadline_clear))
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { dismissThen(onDismiss) }) {
                    Text(stringResource(R.string.deadline_picker_cancel))
                }
                TextButton(onClick = {
                    val millis = selectedMillis()
                    dismissThen { onDeadlinePicked(millis) }
                }) {
                    Text(stringResource(R.string.deadline_picker_confirm))
                }
            }
        }
    }
}

// -- Wheel -------------------------------------------------------------------

/**
 * A single snap-scrolling wheel over [valueCount] distinct values, reported via
 * [onCentered] (always in `0 until valueCount`). The centered row is drawn in
 * the accent color; neighbors fade with distance.
 *
 * Centering trick: [WHEEL_VISIBLE_ROWS]/2 blank spacer rows pad each end, so a
 * raw item index `r` is dead-center exactly when the list's first visible index
 * is `r`. That makes `animateScrollToItem(r)` center it, and the centered raw
 * index is read straight off the scroll position.
 *
 * When [cyclic], the values are repeated [WHEEL_REPEAT] times and the displayed
 * value is `rawIndex % valueCount`, so scrolling wraps endlessly.
 */
@Composable
private fun WheelColumn(
    state: LazyListState,
    valueCount: Int,
    cyclic: Boolean,
    onCentered: (Int) -> Unit,
    label: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val half = WHEEL_VISIBLE_ROWS / 2
    val rowHeightPx = with(LocalDensity.current) { WheelRowHeight.toPx() }
    val itemCount = if (cyclic) valueCount * WHEEL_REPEAT else valueCount

    val centeredRaw by remember(state, itemCount) {
        derivedStateOf {
            val first = state.firstVisibleItemIndex
            val offset = state.firstVisibleItemScrollOffset
            (if (offset > rowHeightPx / 2) first + 1 else first).coerceIn(0, itemCount - 1)
        }
    }
    val centeredValue = if (cyclic) centeredRaw % valueCount else centeredRaw
    LaunchedEffect(centeredValue) { onCentered(centeredValue) }

    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(WheelRowHeight * WHEEL_VISIBLE_ROWS),
    ) {
        items(itemCount + half * 2) { listIndex ->
            val rawIndex = listIndex - half
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WheelRowHeight),
                contentAlignment = Alignment.Center,
            ) {
                if (rawIndex in 0 until itemCount) {
                    val distance = abs(rawIndex - centeredRaw)
                    val isCenter = distance == 0
                    val alpha = when (distance) {
                        0 -> 1f
                        1 -> 0.5f
                        2 -> 0.3f
                        else -> 0.18f
                    }
                    Text(
                        text = label(if (cyclic) rawIndex % valueCount else rawIndex),
                        maxLines = 1,
                        style = if (isCenter) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        fontWeight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCenter) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                        },
                    )
                }
            }
        }
    }
}

/**
 * For a cyclic wheel currently centered near raw index [currentRaw], returns the
 * raw index nearest the current position whose value equals [targetValue].
 */
private fun nearestCyclicIndex(currentRaw: Int, valueCount: Int, targetValue: Int): Int {
    val base = currentRaw - Math.floorMod(currentRaw, valueCount)
    return listOf(base + targetValue - valueCount, base + targetValue, base + targetValue + valueCount)
        .minByOrNull { abs(it - currentRaw) }!!
        .coerceIn(0, valueCount * WHEEL_REPEAT - 1)
}

// -- Labels & presets --------------------------------------------------------

private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
private val dateWithYearFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

private fun dateLabel(
    date: LocalDate,
    today: LocalDate,
    todayLabel: String,
    tomorrowLabel: String,
): String = when (date) {
    today -> todayLabel
    today.plusDays(1) -> tomorrowLabel
    // Show the year once the date leaves the current year, so it stays unambiguous.
    else -> date.format(if (date.year == today.year) dateFormatter else dateWithYearFormatter)
}

private data class DeadlinePreset(val labelRes: Int, val moment: LocalDateTime)

/** Relative-time shortcuts, anchored to [today]. */
private fun presets(today: LocalDate): List<DeadlinePreset> = listOf(
    DeadlinePreset(R.string.deadline_preset_today, today.atTime(18, 0)),
    DeadlinePreset(R.string.deadline_preset_tonight, today.atTime(21, 0)),
    DeadlinePreset(R.string.deadline_preset_tomorrow, today.plusDays(1).atTime(9, 0)),
    DeadlinePreset(
        R.string.deadline_preset_weekend,
        today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).atTime(10, 0),
    ),
    DeadlinePreset(
        R.string.deadline_preset_next_week,
        today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(9, 0),
    ),
)
