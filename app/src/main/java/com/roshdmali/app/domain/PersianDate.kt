package com.roshdmali.app.domain

import java.time.LocalDate

data class PersianDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

fun todayAsPersianDate(): PersianDate = LocalDate.now().toEpochDay().toPersianDate()

fun Long.toPersianDate(): PersianDate {
    val gregorian = LocalDate.ofEpochDay(this)
    var year = gregorian.year - 621
    val (calculatedGregorianYear, marchDay, leap) = jalaliCalendar(year)
    val firstDay = gregorianToJulianDay(calculatedGregorianYear, 3, marchDay)
    var dayOffset = epochDayToJulianDay(this) - firstDay

    if (dayOffset < 0) {
        year -= 1
        dayOffset += 179
        if (leap == 1) dayOffset += 1
        val month = 7 + (dayOffset / 30).toInt()
        val day = (dayOffset % 30).toInt() + 1
        return PersianDate(year, month, day)
    }

    if (dayOffset <= 185) {
        val month = 1 + (dayOffset / 31).toInt()
        val day = (dayOffset % 31).toInt() + 1
        return PersianDate(year, month, day)
    }

    dayOffset -= 186
    val month = 7 + (dayOffset / 30).toInt()
    val day = (dayOffset % 30).toInt() + 1
    return PersianDate(year, month, day)
}

fun PersianDate.toEpochDayOrNull(): Long? {
    if (year !in JALALI_BREAKS.first() + 1 until JALALI_BREAKS.last()) return null
    if (month !in 1..12 || day !in 1..daysInJalaliMonth(year, month)) return null

    val (gregorianYear, marchDay) = jalaliCalendar(year)
    val monthOffset = if (month <= 7) {
        (month - 1) * 31
    } else {
        6 * 31 + (month - 7) * 30
    }
    val julianDay = gregorianToJulianDay(gregorianYear, 3, marchDay) + monthOffset + day - 1
    return julianDay - UNIX_EPOCH_JULIAN_DAY
}

fun Long.formatPersianDate(): String {
    val date = toPersianDate()
    return "${date.year.toString().toPersianDigits()}/${date.month.toString().padStart(2, '0').toPersianDigits()}/${date.day.toString().padStart(2, '0').toPersianDigits()}"
}

fun String.parsePersianDateEpochDay(): Long? {
    val normalized = map { character ->
        when (character) {
            in '۰'..'۹' -> (character.code - '۰'.code + '0'.code).toChar()
            in '٠'..'٩' -> (character.code - '٠'.code + '0'.code).toChar()
            else -> character
        }
    }.joinToString("")
    val parts = normalized.split('/', '-', '.')
    if (parts.size != 3 || parts.any(String::isBlank)) return null
    return PersianDate(
        year = parts[0].toIntOrNull() ?: return null,
        month = parts[1].toIntOrNull() ?: return null,
        day = parts[2].toIntOrNull() ?: return null,
    ).toEpochDayOrNull()
}

private fun String.toPersianDigits(): String = map { character ->
    if (character in '0'..'9') (character.code + ('۰'.code - '0'.code)).toChar() else character
}.joinToString("")

private fun daysInJalaliMonth(year: Int, month: Int): Int = when {
    month <= 6 -> 31
    month <= 11 -> 30
    isLeapJalaliYear(year) -> 30
    else -> 29
}

private fun isLeapJalaliYear(year: Int): Boolean = jalaliCalendar(year).third == 0

private fun jalaliCalendar(year: Int): Triple<Int, Int, Int> {
    require(year >= JALALI_BREAKS.first() && year < JALALI_BREAKS.last())
    val gregorianYear = year + 621
    var leapJalali = -14
    var previousBreak = JALALI_BREAKS.first()
    var jump = 0

    for (index in 1 until JALALI_BREAKS.size) {
        val nextBreak = JALALI_BREAKS[index]
        jump = nextBreak - previousBreak
        if (year < nextBreak) break
        leapJalali += (jump / 33) * 8 + ((jump % 33) / 4)
        previousBreak = nextBreak
    }

    var yearOffset = year - previousBreak
    leapJalali += (yearOffset / 33) * 8 + ((yearOffset % 33 + 3) / 4)
    if (jump % 33 == 4 && jump - yearOffset == 4) leapJalali += 1

    val leapGregorian = gregorianYear / 4 - ((gregorianYear / 100 + 1) * 3) / 4 - 150
    val marchDay = 20 + leapJalali - leapGregorian
    if (jump - yearOffset < 6) {
        yearOffset = yearOffset - jump + (jump + 4) / 33 * 33
    }
    var leap = ((yearOffset + 1) % 33 - 1) % 4
    if (leap == -1) leap = 4

    return Triple(gregorianYear, marchDay, leap)
}

private fun gregorianToJulianDay(year: Int, month: Int, day: Int): Long {
    val adjustment = (14 - month) / 12
    val adjustedYear = year + 4800 - adjustment
    val adjustedMonth = month + 12 * adjustment - 3
    return day.toLong() + (153L * adjustedMonth + 2) / 5 + 365L * adjustedYear +
        adjustedYear / 4 - adjustedYear / 100 + adjustedYear / 400 - 32045
}

private fun epochDayToJulianDay(epochDay: Long): Long = epochDay + UNIX_EPOCH_JULIAN_DAY

private const val UNIX_EPOCH_JULIAN_DAY = 2_440_588L
private val JALALI_BREAKS = listOf(
    -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
    1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
)