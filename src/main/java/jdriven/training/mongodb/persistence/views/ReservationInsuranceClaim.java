package jdriven.training.mongodb.persistence.views;

import java.time.LocalDate;

public record ReservationInsuranceClaim(
		LocalDate date,
		String chalet,
		String damage,
		boolean hasInsurance
) {
}
