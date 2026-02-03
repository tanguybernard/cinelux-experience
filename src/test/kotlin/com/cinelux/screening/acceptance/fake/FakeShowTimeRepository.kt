package com.cinelux.screening.acceptance.fake

import com.cinelux.screening.application.port.spi.ShowTimeRepository
import com.cinelux.screening.domain.model.ShowTime
import java.time.DayOfWeek

class FakeShowTimeRepository : ShowTimeRepository {
    private val showTimes = mutableListOf<ShowTime>()

    override fun findByDayOfWeek(dayOfWeek: DayOfWeek): List<ShowTime> {
        return showTimes.filter { it.dayOfWeek == dayOfWeek }
    }

    fun addShowTime(showTime: ShowTime) {
        showTimes.add(showTime)
    }

    fun clear() {
        showTimes.clear()
    }
}
