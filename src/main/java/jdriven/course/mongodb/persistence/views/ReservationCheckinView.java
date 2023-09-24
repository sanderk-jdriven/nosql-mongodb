package jdriven.course.mongodb.persistence.views;

import java.time.LocalDate;
import java.util.List;

public record ReservationCheckinView(
		LocalDate date,
		List<String> bookers
) {}
