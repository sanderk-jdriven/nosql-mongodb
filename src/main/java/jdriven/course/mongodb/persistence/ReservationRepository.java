package jdriven.course.mongodb.persistence;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import jdriven.course.mongodb.config.MongoDbIdProvider;
import jdriven.course.mongodb.persistence.views.ReservationCheckin;
import jdriven.course.mongodb.persistence.views.ReservationIncomeSummary;
import jdriven.course.mongodb.persistence.views.ReservationInsuranceClaim;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Welcome to our humble abode(s). We host a selection of chalets visited by guests all across
 * the globe. Would you help us implement some of the database interactions required for us to
 * do our job? Some functionalities are already implemented, others are not. We did take our
 * time to write a test suite and comments containing our expectations!
 */
@Service
@AllArgsConstructor
public class ReservationRepository {

    private final MongoTemplate mongo;
    private final MongoDbIdProvider id;

    public Optional<ReservationEntity> find(UUID id) {
        return Optional.ofNullable(mongo.findById(id, ReservationEntity.class));
    }

    public List<ReservationEntity> findAll() {
        return mongo.findAll(ReservationEntity.class);
    }

    public ReservationEntity save(ReservationEntry entry) {
        return mongo.save(createEntity(entry));
    }

    public void delete(UUID id) {
        mongo.remove(id);
    }

    /*
     * Queries allow us to retrieve documents according to our criteria, limit the number of
     * documents retrieved, perform sorting, limit the fields returned from the document (Projections),
     * query by example or perform pagination. A lot is possible, but queries are still limited in
     * functionality. For more complex queries, we will move to aggregate pipelines later on.
     */

    /**
     * Occasionally we would like to retrieve the ten most expensive reservations of the specified
     * day. We do this since would like to make them an offer, such that they might return. Please
     * help us fetch this data sorted by price descending and excluding free stays.
     */
    public List<ReservationEntity> queryExample_mostExpensive(LocalDate date) {
        var query = new Query();
        query.limit(10);
        query.with(Sort.by(Sort.Order.desc("price")));
        query.addCriteria(Criteria.where("price").gt(0));
        query.addCriteria(Criteria.where("date").is(date));
        return mongo.find(query, ReservationEntity.class);
    }

    /**
     * Chalets get damaged, but when it happens due to mishandling by the guests, they will have
     * to pay for the damages. For each reservation we track damages, which can be filed by
     * staff. Please help us fetch all reservations with damages for the given chalet, but which
     * do not have insurance. When a reservation has no damages, the damages field will be null.
     */
    public List<ReservationEntity> queryExercise_damageClaims(String chalet) {
        throw new NotImplementedException();
    }

    /**
     * Our user interface requires us to retrieve a list of reservations, sorted by reservation
     * date in the selected order, pageable and with the ability to search by partial name
     * of the booker. Even though mongodb might not be the best database for text searches, we
     * still have some options.
     */
    public List<ReservationEntity> queryExercise_pageAndSort(int pageSize, int page, boolean ascending, String booker) {
        throw new NotImplementedException();
    }

    /*
     * Updates allow us to change fields, add or remove from arrays, increment numbers and more  in
     * documents matching our criteria. You will need the knowledge from the previous exercises to
     * implement the update functionalities.
     */

    /**
     * Sometimes, bookers make a mistake and enter their name incorrectly. This way the information we
     * have won't match their identification, and the checkin process is delayed. We don't want people
     * waiting, so we allow them to update their name.
     */
    public void updateExample_bookerCorrection(UUID id, String name) {
        var query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));

        var update = new Update();
        update.set("booker", name);

        mongo.updateFirst(query, update, ReservationEntity.class);
    }

    /**
     * A reservation can be expanded, but is seldom made smaller. Therefore, we would like to be able
     * to add a new guest to a reservation with the specified id. This means that a new name will be
     * added to the array of guests.
     */
    public void updateExercise_includeNewGuests(UUID id, String guest) {
        throw new NotImplementedException();
    }

    /**
     * Each year, for the birthday of this company, all reservations for the given day are given
     * a $50 discount. The only problem is that the price of a reservation is including insurance,
     * which is not part of the discount. Therefore, the discount should only be applied if:
     * - The reservation costs >= $300 with insurance
     * - The reservation costs >= $250 without insurance
     * - The reservation must not have been paid for yet, otherwise we'll have to refund
     * @implNote you will need to use the 'andOperator' and 'orOperator' for composing complex criteria.
     */
    public void updateExercise_anniversaryDiscount(LocalDate date) {
        throw new NotImplementedException();
    }

    /*
     * Aggregate pipelines allow us to perform more complex queries, aggregations, groupings,
     * create new fields, create completely new documents, and much more. This is the most powerful
     * method to retrieve data from the database, but it's more complex. Therefor, it is important
     * to use queries and updates wherever possible.
     */

    /**
     * At the start of the day, we print a list of all the bookers that will check in for that day. Therefore,
     * we would like to have a pipeline to generate a list of the names of all the bookers we can expect to
     * check in on the specified date.
     */
    public ReservationCheckin pipelineExample_checkinList(LocalDate date) {
        var pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("date").is(date)),
                Aggregation.group("date").addToSet("booker").as("bookers"),
                Aggregation.addFields().addField("date").withValue(date).build()
        );

        return mongo.aggregate(pipeline, ReservationEntity.class, ReservationCheckin.class).getUniqueMappedResult();
    }

    /**
     * Each month we would like to register how much income we have generated. Therefore, we would like to
     * create a pipeline which selects all the reservations from the specified month in the specified year,
     * and calculate our total income for that month. We should store this document in another collection
     * and return it. If we create a view of the same month twice, we should overwrite the existing view.
     */
    public Optional<ReservationIncomeSummary> pipelineExercise_incomeGenerated(Year year, Month month) {
        throw new NotImplementedException();
    }

    /**
     * The insurance company wants to have an individual damage report for each damage at each chalet,
     * since they have to process each insurance claim individually. Create a pipeline which retrieves
     * all the chalets that have damages, and create individual reports for each damage in the list of
     * damages. Only include reservations with damages at the specified date. Include only those that
     * had insurance.
     */
    public List<ReservationInsuranceClaim> pipelineExercise_insuranceClaims(List<String> chalets, LocalDate date) {
        throw new NotImplementedException();
    }

    private ReservationEntity createEntity(ReservationEntry entry) {
        return ReservationEntity.builder()
                .id(id.provide())
                .price(entry.price())
                .date(entry.date())
                .chalet(entry.chalet())
                .booker(entry.booker())
                .guests(entry.guests())
                .hasPaid(entry.hasPaid())
                .hasInsurance(entry.hasInsurance())
                .build();
    }
}
