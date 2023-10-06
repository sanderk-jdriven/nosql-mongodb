package jdriven.training.mongodb.persistence;

import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;
import lombok.Builder;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Document("reservation")
public record ReservationEntity(
		@MongoId
		UUID id,
		@NonNull
		Long price,
		@NonNull
		LocalDate date,
		@NonNull
		String chalet,
		@NonNull
		String booker,
		@NonNull
		List<String> guests,
		@NonNull
		Boolean hasPaid,
		@NonNull
		Boolean hasInsurance,
		@Nullable
		List<String> damages
) {}
