package com.cinelux.screening.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ShowTimeTest {

    @Test
    fun `should create showtime with correct day of week`() {
        // Given a Monday at 14:00
        val monday = LocalDate.of(2024, 1, 8) // January 8, 2024 is a Monday
            .atTime(14, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        // When
        val showTime = ShowTime.create(
            id = ShowTimeId("show-1"),
            movieTitle = "The Matrix",
            hall = Hall(id = "hall-1", name = "Hall 1"),
            startTime = monday
        )

        // Then
        assertEquals(DayOfWeek.MONDAY, showTime.dayOfWeek)
        assertEquals("The Matrix", showTime.movieTitle)
        assertEquals("Hall 1", showTime.hall.name)
        assertNotNull(showTime.startTime)
    }

    @Test
    fun `should reject blank movie title`() {
        val exception = assertThrows<IllegalArgumentException> {
            ShowTime.create(
                id = ShowTimeId("show-1"),
                movieTitle = "  ",
                hall = Hall(id = "hall-1", name = "Hall 1"),
                startTime = Instant.now()
            )
        }

        assertEquals("Movie title cannot be blank", exception.message)
    }

    @Test
    fun `should reject blank showtime id`() {
        val exception = assertThrows<IllegalArgumentException> {
            ShowTimeId("")
        }

        assertEquals("ShowTimeId cannot be blank", exception.message)
    }

    @Test
    fun `should reject blank hall id`() {
        val exception = assertThrows<IllegalArgumentException> {
            Hall(id = "", name = "Hall 1")
        }

        assertEquals("Hall id cannot be blank", exception.message)
    }

    @Test
    fun `should reject blank hall name`() {
        val exception = assertThrows<IllegalArgumentException> {
            Hall(id = "hall-1", name = "")
        }

        assertEquals("Hall name cannot be blank", exception.message)
    }
}
