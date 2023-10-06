package jdriven.training.mongodb.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import jdriven.training.mongodb.persistence.views.ReservationIncomeSummary;

import java.time.LocalDate;
import java.time.Year;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class ReservationRepositoryTest {

	private static final LocalDate DATE = LocalDate.of(2023, 1, 1);

	@Autowired
	private MongoTemplate mongo;

	private ReservationRepository repository;

	@Container
	static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:6.0.3")
			.withEnv("MONGO_INITDB_DATABASE", "test");

	@DynamicPropertySource
	static void register(DynamicPropertyRegistry registry) {
		registry.add("mongodb.url", mongoContainer::getConnectionString);
		registry.add("mongodb.database", () -> "test");
	}

	@BeforeAll
	static void setup() {
		mongoContainer.start();
	}

	@AfterAll
	static void tearDown() {
		mongoContainer.stop();
	}

	@BeforeEach
	void setUp() {
		repository = new ReservationRepository(mongo, UUID::randomUUID);
	}

	@AfterEach
	void clear() {
		mongo.findAllAndRemove(new Query(), ReservationEntity.class);
	}

	@Test
	void queryExample_mostExpensive() {
		var topTen = IntStream.range(10, 20)
				.mapToObj(ReservationRepositoryTest::reservationWithPrice)
				.map(mongo::save)
				.collect(Collectors.toList());

		IntStream.range(1, 10)
				.mapToObj(ReservationRepositoryTest::reservationWithPrice)
				.forEach(mongo::save);

		var result = repository.queryExample_mostExpensive(DATE);

		Collections.reverse(topTen);
		assertThat(result).isEqualTo(topTen);
	}

	@Test
	void queryExercise_damageClaims() {
		var withDamagesAndInsurance = reservationWithDamages("120A", true, List.of("broken window"));
		var withDamagesWithoutInsurance = reservationWithDamages("120A", false, List.of("broken table"));
		var withoutDamagesOrInsurance = reservationWithDamages("120A", false, null);
		var withoutDamagesWithInsurance = reservationWithDamages("120A", true, null);
		var withDamagesWithoutInsuranceOtherChalet = reservationWithDamages("100B", false, List.of("broken sink"));

		mongo.save(withDamagesAndInsurance);
		mongo.save(withDamagesWithoutInsurance);
		mongo.save(withoutDamagesOrInsurance);
		mongo.save(withoutDamagesWithInsurance);
		mongo.save(withDamagesWithoutInsuranceOtherChalet);

		var result = repository.queryExercise_damageClaims("120A");

		assertThat(result).containsExactly(withDamagesWithoutInsurance);
	}

	@Test
	void queryExercise_pageAndSort() {
		var thomas = mongo.save(reservationWithBookerAndDate("thomas", DATE));
		var anouk = mongo.save(reservationWithBookerAndDate("anouk", DATE));
		var will = mongo.save(reservationWithBookerAndDate("will", DATE));
		var rebecca = mongo.save(reservationWithBookerAndDate("rebecca", DATE.minusDays(1)));
		var john = mongo.save(reservationWithBookerAndDate("john", DATE.minusDays(1)));

		mongo.save(thomas);
		mongo.save(anouk);
		mongo.save(will);
		mongo.save(rebecca);
		mongo.save(john);

		var resultAsc = repository.queryExercise_pageAndSort(2, 0, true, "o");
		assertThat(resultAsc).containsExactly(john, thomas);

		var resultAscNextPage = repository.queryExercise_pageAndSort(2, 1, true, "o");
		assertThat(resultAscNextPage).containsExactly(anouk);

		var resultDesc = repository.queryExercise_pageAndSort(2, 0, false, "o");
		assertThat(resultDesc).containsExactly(thomas, anouk);

		var resultDescNextPage = repository.queryExercise_pageAndSort(2, 1, false, "o");
		assertThat(resultDescNextPage).containsExactly(john);
	}

	@Test
	void updateExample_bookerCorrection() {
		var stored = mongo.save(randomReservation());

		repository.updateExample_bookerCorrection(stored.id(), "jan");

		var updated = mongo.findById(stored.id(), ReservationEntity.class);
		assertThat(updated).isNotNull();
		assertThat(updated.booker()).isEqualTo("jan");
	}

	@Test
	void updateExercise_includeNewGuests() {
		var stored = mongo.save(reservationWithGuests("jan"));

		repository.updateExercise_includeNewGuests(stored.id(), "aad");

		var updated = mongo.findById(stored.id(), ReservationEntity.class);
		assertThat(updated).isNotNull();
		assertThat(updated.guests()).containsExactly("jan", "aad");
	}

	@Test
	@SuppressWarnings("ConstantConditions")
	void updateExercise_anniversaryDiscount() {
		var eligibleWithInsurance = reservationForBirthday(DATE, 325, false, true);
		var ineligibleWithInsurance = reservationForBirthday(DATE, 275, false, true);
		var eligibleWithoutInsurance = reservationForBirthday(DATE, 275, false, false);
		var ineligibleWithoutInsurance = reservationForBirthday(DATE, 225, false, false);
		var eligibleButHasPaid = reservationForBirthday(DATE, 275, true, false);
		var eligibleButOtherDate = reservationForBirthday(DATE.minusDays(1), 275, false, false);

		mongo.save(eligibleWithInsurance);
		mongo.save(ineligibleWithInsurance);
		mongo.save(eligibleWithoutInsurance);
		mongo.save(ineligibleWithoutInsurance);
		mongo.save(eligibleButHasPaid);
		mongo.save(eligibleButOtherDate);

		repository.updateExercise_anniversaryDiscount(DATE);

		assertThat(mongo.findById(eligibleWithInsurance.id(), ReservationEntity.class).price()).isEqualTo(275);
		assertThat(mongo.findById(ineligibleWithInsurance.id(), ReservationEntity.class).price()).isEqualTo(275);
		assertThat(mongo.findById(eligibleWithoutInsurance.id(), ReservationEntity.class).price()).isEqualTo(225);
		assertThat(mongo.findById(ineligibleWithoutInsurance.id(), ReservationEntity.class).price()).isEqualTo(225);
		assertThat(mongo.findById(eligibleButHasPaid.id(), ReservationEntity.class).price()).isEqualTo(275);
		assertThat(mongo.findById(eligibleButOtherDate.id(), ReservationEntity.class).price()).isEqualTo(275);
	}

	@Test
	void pipelineExample_checkinList() {
		var thomas = mongo.save(reservationWithBooker("thomas"));
		var anouk = mongo.save(reservationWithBooker("anouk"));

		var result = repository.pipelineExample_checkinList(DATE);

		assertThat(result.date()).isEqualTo(DATE);
		assertThat(result.bookers()).contains(thomas.booker(), anouk.booker());
	}

	@Test
	void pipelineExercise_incomeGenerated() {
		var thomas = mongo.save(reservationWithBooker("thomas"));
		var anouk = mongo.save(reservationWithBooker("anouk"));

		var year = Year.of(DATE.getYear());
		var month = DATE.getMonth();

		var result = repository.pipelineExercise_incomeGenerated(year, month).orElseThrow();

		assertThat(result.id()).isEqualTo(year.atMonth(month).toString());
		assertThat(result.income()).isEqualTo(thomas.price() + anouk.price());
		assertThat(mongo.findOne(new Query(), ReservationIncomeSummary.class)).isEqualTo(result);
	}

	@Test
	void pipelineExercise_insuranceClaims() {
		var largeGroupInChaletList = reservationWithInsuranceAndChaletAndGuests("100B", "broken window", "broken sink");
		var smallGroupInChaletList = reservationWithInsuranceAndChaletAndGuests("110B", "broken lamp");
		var largeGroupNotInChaletList = reservationWithInsuranceAndChaletAndGuests("120B", "broken tile");
		var smallGroupNotInChaletList = reservationWithInsuranceAndChaletAndGuests("130B", "broken vacuum");

		mongo.save(largeGroupInChaletList);
		mongo.save(smallGroupInChaletList);
		mongo.save(largeGroupNotInChaletList);
		mongo.save(smallGroupNotInChaletList);

		var result = repository.pipelineExercise_insuranceClaims(List.of("100B", "110B"), DATE);

		assertThat(result.size()).isEqualTo(3);
		assertThat(result.get(0).chalet()).isEqualTo("100B");
		assertThat(result.get(1).chalet()).isEqualTo("100B");
		assertThat(result.get(2).chalet()).isEqualTo("110B");
		assertThat(result.get(0).date()).isEqualTo(DATE);
		assertThat(result.get(1).date()).isEqualTo(DATE);
		assertThat(result.get(2).date()).isEqualTo(DATE);
		assertThat(result.get(0).damage()).isEqualTo("broken window");
		assertThat(result.get(1).damage()).isEqualTo("broken sink");
		assertThat(result.get(2).damage()).isEqualTo("broken lamp");
	}

	private static ReservationEntity randomReservation() {
		return ReservationEntity.builder()
				.id(UUID.randomUUID())
				.price(120L)
				.date(DATE)
				.chalet("chalet")
				.booker("booker")
				.guests(List.of("kai", "jack"))
				.hasPaid(true)
				.hasInsurance(false)
				.damages(null)
				.build();
	}

	private static ReservationEntity reservationWithPrice(long price) {
		return ReservationEntity.builder()
				.id(UUID.randomUUID())
				.price(price)
				.date(DATE)
				.chalet("80C")
				.booker("booker")
				.guests(List.of("kai", "jack"))
				.hasPaid(true)
				.hasInsurance(false)
				.damages(null)
				.build();
	}

	private static ReservationEntity reservationWithGuests(String... guests) {
		return ReservationEntity.builder()
				.id(UUID.randomUUID())
				.price(120L)
				.date(DATE)
				.chalet("80C")
				.booker("booker")
				.guests(Arrays.asList(guests))
				.hasPaid(true)
				.hasInsurance(false)
				.damages(null)
				.build();
	}

	private static ReservationEntity reservationWithBooker(String booker) {
		return ReservationEntity.builder()
				.id(UUID.randomUUID())
				.price(120L)
				.date(DATE)
				.chalet("80C")
				.booker(booker)
				.guests(List.of("kai", "jack"))
				.hasPaid(true)
				.hasInsurance(false)
				.damages(null)
				.build();
	}

	private static ReservationEntity reservationWithInsuranceAndChaletAndGuests(String chalet, String... damages) {
		return ReservationEntity.builder()
				.id(UUID.randomUUID())
				.price(120L)
				.date(DATE)
				.chalet(chalet)
				.booker("booker")
				.guests(List.of("jan", "rebecca"))
				.hasPaid(true)
				.hasInsurance(true)
				.damages(Arrays.asList(damages))
				.build();
	}

	private static ReservationEntity reservationWithBookerAndDate(String booker, LocalDate date) {
		return ReservationEntity.builder()
				.id(UUID.randomUUID())
				.price(120L)
				.date(date)
				.chalet("80C")
				.booker(booker)
				.guests(List.of("kai", "jack"))
				.hasPaid(true)
				.hasInsurance(false)
				.damages(null)
				.build();
	}

	private static ReservationEntity reservationWithDamages(String chalet, boolean hasInsurance, List<String> damages) {
		return ReservationEntity.builder()
				.id(UUID.randomUUID())
				.price(20L)
				.date(DATE)
				.chalet(chalet)
				.booker("alex")
				.guests(List.of("kai", "jack"))
				.hasPaid(true)
				.hasInsurance(hasInsurance)
				.damages(damages)
				.build();
	}

	private static ReservationEntity reservationForBirthday(LocalDate date, long price, boolean hasPaid, boolean hasInsurance) {
		return ReservationEntity.builder()
				.id(UUID.randomUUID())
				.price(price)
				.date(date)
				.chalet("120B")
				.booker("alex")
				.guests(List.of("kai", "jack"))
				.hasPaid(hasPaid)
				.hasInsurance(hasInsurance)
				.damages(null)
				.build();
	}


}
