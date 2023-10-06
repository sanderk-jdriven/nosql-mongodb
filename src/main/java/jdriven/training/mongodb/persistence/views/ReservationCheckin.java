package jdriven.training.mongodb.persistence.views;

import java.time.LocalDate;
import java.util.List;

public record ReservationCheckin(
		LocalDate date,
		List<String> bookers
) {}
