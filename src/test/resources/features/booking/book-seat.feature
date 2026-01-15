Feature: Book a Cinema Seat

  As a cinema customer
  I want to book a specific seat for a movie showing
  So that I can guarantee my place at the cinema

  Background:
    Given "The Matrix" is showing at "19:00" in "Hall 1"
    And the hall has the following seats:
      | Row | Numbers |
      | A   | 1-10    |
      | B   | 1-10    |
      | C   | 1-8     |

  Scenario: Successfully book an available seat
    Given I am customer "Alice"
    When I book seat "B4" for "The Matrix" at "19:00"
    Then the booking should be confirmed
    And I should receive a booking confirmation with:
      | Seat  | B4         |
      | Movie | The Matrix |
    And seat "B4" should be booked for "The Matrix" at "19:00"

  Scenario: Cannot book an already booked seat
    Given I am customer "Alice"
    And seat "B4" is already booked for "The Matrix" at "19:00"
    When I book seat "B4" for "The Matrix" at "19:00"
    Then the booking should fail with "Seat already booked"

  Scenario: Cannot book a seat that does not exist in the hall
    Given I am customer "Alice"
    When I book seat "D1" for "The Matrix" at "19:00"
    Then the booking should fail with "Seat not in hall"

  Scenario: Cannot book for a non-existent showtime
    Given I am customer "Alice"
    When I book seat "A1" for "Unknown Movie" at "20:00"
    Then the booking should fail with "Showtime not found"

  Scenario: Multiple customers can book different seats
    Given I am customer "Alice"
    And seat "A1" is already booked for "The Matrix" at "19:00"
    When I book seat "A2" for "The Matrix" at "19:00"
    Then the booking should be confirmed
    And seat "A1" should be booked for "The Matrix" at "19:00"
    And seat "A2" should be booked for "The Matrix" at "19:00"
