package jdriven.course.mongodb.persistence;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record ReservationEntry(
		Long price,
		LocalDate date,
		String chalet,
		String booker,
		List<String> guests,
		Boolean hasPaid,
		Boolean hasInsurance
) {}
