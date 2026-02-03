package com.cinelux.screening.application.port.spi

import com.cinelux.screening.domain.model.ShowTime
import java.time.DayOfWeek

interface ShowTimeRepository {
    fun findByDayOfWeek(dayOfWeek: DayOfWeek): List<ShowTime>
}
