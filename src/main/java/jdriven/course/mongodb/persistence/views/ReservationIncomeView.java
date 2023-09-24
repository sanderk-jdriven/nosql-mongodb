package jdriven.course.mongodb.persistence.views;

import com.mongodb.lang.NonNull;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("reservation-income-view")
public record ReservationIncomeView(
		@MongoId
		String id,
		@NonNull
		Long income
) {}
