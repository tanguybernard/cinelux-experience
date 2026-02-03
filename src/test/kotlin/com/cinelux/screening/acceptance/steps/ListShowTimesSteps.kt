package com.cinelux.screening.acceptance.steps

import com.cinelux.screening.acceptance.ScreeningTestContext
import com.cinelux.screening.application.port.api.ListShowTimesQuery
import com.cinelux.screening.application.port.api.ListShowTimesResult
import com.cinelux.screening.application.port.api.ListShowTimesUseCase
import com.cinelux.screening.application.usecase.ListShowTimesUseCaseImpl
import com.cinelux.screening.domain.model.Hall
import com.cinelux.screening.domain.model.ShowTime
import com.cinelux.screening.domain.model.ShowTimeId
import io.cucumber.datatable.DataTable
import io.cucumber.java.Before
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID

class ListShowTimesSteps {

    private lateinit var listShowTimesUseCase: ListShowTimesUseCase
    private var result: ListShowTimesResult? = null

    @Before(order = 0)
    fun setup() {
        ScreeningTestContext.reset()
        listShowTimesUseCase = ListShowTimesUseCaseImpl(
            ScreeningTestContext.showTimeRepository
        )
    }

    @Given("the following showtimes are scheduled:")
    fun theFollowingShowTimesAreScheduled(dataTable: DataTable) {
        dataTable.asMaps().forEach { row ->
            val movieTitle = row["Movie"]!!
            val dayOfWeek = DayOfWeek.valueOf(row["Day"]!!)
            val time = row["Time"]!!
            val hallName = row["Hall"]!!

            val showTime = createShowTime(movieTitle, dayOfWeek, time, hallName)
            ScreeningTestContext.showTimeRepository.addShowTime(showTime)
        }
    }

    @When("I list showtimes for {string}")
    fun iListShowTimesFor(day: String) {
        val dayOfWeek = DayOfWeek.valueOf(day)
        result = listShowTimesUseCase.execute(ListShowTimesQuery(dayOfWeek))
    }

    @Then("I should see {int} showtime(s)")
    fun iShouldSeeShowTimes(expectedCount: Int) {
        assertThat(result).isNotNull
        assertThat(result!!.count).isEqualTo(expectedCount)
    }

    @And("the showtimes should include:")
    fun theShowTimesShouldInclude(dataTable: DataTable) {
        assertThat(result).isNotNull
        dataTable.asMaps().forEach { row ->
            val movieTitle = row["Movie"]!!
            val time = row["Time"]!!
            val hallName = row["Hall"]!!

            val matchingShowTime = result!!.showTimes.find { dto ->
                dto.movieTitle == movieTitle &&
                dto.hallName == hallName &&
                formatTime(dto.startTime) == time
            }
            assertThat(matchingShowTime)
                .withFailMessage("Expected showtime for $movieTitle at $time in $hallName")
                .isNotNull
        }
    }

    @And("the result should be empty")
    fun theResultShouldBeEmpty() {
        assertThat(result).isNotNull
        assertThat(result!!.isEmpty()).isTrue()
    }

    private fun createShowTime(
        movieTitle: String,
        dayOfWeek: DayOfWeek,
        time: String,
        hallName: String
    ): ShowTime {
        val (hour, minute) = time.split(":").map { it.toInt() }
        val startTime = LocalDate.now()
            .with(TemporalAdjusters.nextOrSame(dayOfWeek))
            .atTime(hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        return ShowTime.create(
            id = ShowTimeId("show-${UUID.randomUUID()}"),
            movieTitle = movieTitle,
            hall = Hall(id = hallName.lowercase().replace(" ", "-"), name = hallName),
            startTime = startTime
        )
    }

    private fun formatTime(instant: java.time.Instant): String {
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        return String.format("%02d:%02d", localTime.hour, localTime.minute)
    }
}
