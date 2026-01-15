package com.cinelux.booking.acceptance.fake

import com.cinelux.booking.application.port.spi.ShowTimeRepository
import com.cinelux.booking.domain.model.Seat
import com.cinelux.booking.domain.model.ShowTimeId
import com.cinelux.booking.domain.model.ShowTimeReference
import java.time.Instant

class FakeShowTimeRepository : ShowTimeRepository {

    private val showTimes = mutableMapOf<ShowTimeId, ShowTimeReference>()
    private val seatsByShowTime = mutableMapOf<ShowTimeId, List<Seat>>()

    override fun findById(id: ShowTimeId): ShowTimeReference? {
        return showTimes[id]
    }

    override fun getAllSeatsForShowTime(showTimeId: ShowTimeId): List<Seat> {
        return seatsByShowTime[showTimeId] ?: emptyList()
    }

    fun addShowTime(showTime: ShowTimeReference, seats: List<Seat>) {
        showTimes[showTime.id] = showTime
        seatsByShowTime[showTime.id] = seats
    }

    fun findByMovieAndTime(movieTitle: String, time: String): ShowTimeReference? {
        return showTimes.values.find { it.movieTitle == movieTitle }
    }

    fun clear() {
        showTimes.clear()
        seatsByShowTime.clear()
    }
}
