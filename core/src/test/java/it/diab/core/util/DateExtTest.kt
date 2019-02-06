/*
 * Copyright (c) 2019 Bevilacqua Joey
 *
 * Licensed under the GNU GPLv3 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl.txt
 */
package it.diab.core.util

import it.diab.core.data.entities.TimeFrame
import it.diab.core.util.extensions.asTimeFrame
import it.diab.core.util.extensions.format
import it.diab.core.util.extensions.getCalendar
import it.diab.core.util.extensions.getHour
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class DateExtTest {

    @Test
    fun asTimeFrame() {
        val calendar = Calendar.getInstance()

        calendar[Calendar.HOUR_OF_DAY] = 8
        assertEquals(calendar.time.asTimeFrame(), TimeFrame.MORNING)

        calendar[Calendar.HOUR_OF_DAY] = 18
        assertEquals(calendar.time.asTimeFrame(), TimeFrame.AFTERNOON)

        calendar[Calendar.HOUR_OF_DAY] = 0
        assertEquals(calendar.time.asTimeFrame(), TimeFrame.NIGHT)
    }

    @Test
    fun format() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 1971)
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
        }

        assertEquals(calendar.time.format("yyyy-MM-dd"), "1971-01-01")

        assertEquals(calendar.time.format("HH:mm"), "10:30")
    }

    @Test
    fun getCalendar() {
        val calendar = Calendar.getInstance()
        val date = calendar.time
        assertEquals(date.getCalendar(), calendar)
    }

    @Test
    fun getHour() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
        }

        assertEquals(calendar.time.getHour(), 10)
    }
}