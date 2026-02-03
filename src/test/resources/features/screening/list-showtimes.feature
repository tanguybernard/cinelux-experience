Feature: List Showtimes for a Day

  As a cinema customer
  I want to see all showtimes for a specific day of the week
  So that I can plan my cinema visit

  Background:
    Given the following showtimes are scheduled:
      | Movie           | Day       | Time  | Hall   |
      | The Matrix      | MONDAY    | 14:00 | Hall 1 |
      | The Matrix      | MONDAY    | 19:00 | Hall 1 |
      | Inception       | MONDAY    | 16:00 | Hall 2 |
      | The Dark Knight | TUESDAY   | 20:00 | Hall 1 |
      | Interstellar    | WEDNESDAY | 18:00 | Hall 3 |

  Scenario: List all showtimes for Monday
    When I list showtimes for "MONDAY"
    Then I should see 3 showtimes
    And the showtimes should include:
      | Movie      | Time  | Hall   |
      | The Matrix | 14:00 | Hall 1 |
      | The Matrix | 19:00 | Hall 1 |
      | Inception  | 16:00 | Hall 2 |

  Scenario: List showtimes for a day with single showtime
    When I list showtimes for "TUESDAY"
    Then I should see 1 showtime
    And the showtimes should include:
      | Movie           | Time  | Hall   |
      | The Dark Knight | 20:00 | Hall 1 |

  Scenario: List showtimes for a day with no showtimes
    When I list showtimes for "SUNDAY"
    Then I should see 0 showtimes
    And the result should be empty
