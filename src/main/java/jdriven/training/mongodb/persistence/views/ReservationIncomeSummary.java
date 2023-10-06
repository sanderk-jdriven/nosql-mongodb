package jdriven.training.mongodb.persistence.views;

import com.mongodb.lang.NonNull;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("reservation-income-view")
public record ReservationIncomeSummary(
		@MongoId
		String id,
		@NonNull
		Long income
) {}
