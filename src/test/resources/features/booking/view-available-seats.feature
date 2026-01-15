Feature: View Available Seats for Booking

  As a cinema customer
  I want to see which seats are available for a specific showtime
  So that I can choose a seat to book

  Background:
    Given "The Matrix" is showing at "19:00" in "Hall 1"
    And the hall has the following seats:
      | Row | Numbers |
      | A   | 1-10    |
      | B   | 1-10    |
      | C   | 1-8     |

  Scenario: View all available seats for a showtime with no bookings
    When I view available seats for "The Matrix" at "19:00"
    Then I should see 28 available seats
    And all seats should be marked as available

  Scenario: View available seats when some are already booked
    Given the following seats are already booked for "The Matrix" at "19:00":
      | Seat |
      | A5   |
      | A6   |
      | B3   |
    When I view available seats for "The Matrix" at "19:00"
    Then I should see 25 available seats
    And seats "A5", "A6", "B3" should be marked as unavailable
    And seat "A1" should be marked as available

  Scenario: No available seats when showtime is fully booked
    Given all seats are already booked for "The Matrix" at "19:00"
    When I view available seats for "The Matrix" at "19:00"
    Then I should see 0 available seats
    And I should receive a message "No seats available"
