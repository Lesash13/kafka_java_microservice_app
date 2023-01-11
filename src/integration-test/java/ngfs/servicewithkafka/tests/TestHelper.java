package ngfs.servicewithkafka.tests;

import ngfs.servicewithkafka.model.*;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer2;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;

public class TestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestHelper.class);

    @Value("${spring.kafka.topics.houses}")
    String HOUSES_TOPIC;

    @Value("${spring.kafka.topics.people}")
    String PEOPLE_TOPIC;

    @Value("${spring.kafka.topics.people2}")
    String PEOPLE_TOPIC2;

    protected ConsumerRecord<Integer, PeoplePayload> getLatestRecordInTopic(Consumer<Integer, PeoplePayload> consumer) {

        AtomicLong maxTimestamp = new AtomicLong();
        AtomicReference<ConsumerRecord<Integer, PeoplePayload>> latestRecord = new AtomicReference<>();

        LOGGER.info("Partitions: " + consumer.assignment());

        consumer.poll(Duration.ofSeconds(30)).forEach(record -> {
            LOGGER.info("Record key: " + record.key());
            if (record.timestamp() > maxTimestamp.get()) {
                maxTimestamp.set(record.timestamp());
                latestRecord.set(record);
            }
        });

        LOGGER.info("Latest record in topic: " + latestRecord.get());
        return latestRecord.get();
    }

    protected Map<String, Object> getConsumerConfig(String bootstrapServers) {
        Map<String, Object> consumerConfigs = new HashMap<>(
                KafkaTestUtils.consumerProps(bootstrapServers, "test-consumer-" + Math.random(), "false"));
        consumerConfigs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerConfigs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerConfigs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerConfigs.put(JsonDeserializer.KEY_DEFAULT_TYPE, Integer.class);
        consumerConfigs.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "ngfs.servicewithkafka.model.PeoplePayload");
        consumerConfigs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer2.class);
        consumerConfigs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer2.class);
        consumerConfigs.put(ErrorHandlingDeserializer2.KEY_DESERIALIZER_CLASS, IntegerDeserializer.class);
        consumerConfigs.put(ErrorHandlingDeserializer2.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        return consumerConfigs;
    }

    protected Map<String, Object> getProducerConfig(String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return props;
    }

    protected void recordAssertion(ConsumerRecord<Integer, PeoplePayload> latestRecord, int key,
                                   PeoplePayload payload) {
        assertNotNull("Payload is null, no messgaes found in topic", latestRecord);
        assertEquals("Key is wrong", key, latestRecord.key());
        assertEquals("Event id is wrong", payload.getEventId(), latestRecord.value().getEventId());
        assertEquals("Event type is wrong", payload.getEventType(), latestRecord.value().getEventType());
        assertEquals("Home list is wrong", payload.getEvent().getAddressData().getHomeIds(),
                latestRecord.value().getEvent().getAddressData().getHomeIds());
        assertEquals("Registration id is wrong", payload.getEvent().getAddressData().getRegistrationId(),
                latestRecord.value().getEvent().getAddressData().getRegistrationId());
        assertEquals("Birthday is wrong", payload.getEvent().getBirthday(),
                latestRecord.value().getEvent().getBirthday());
        assertEquals("Children list is wrong", payload.getEvent().getChildren(),
                latestRecord.value().getEvent().getChildren());
        assertEquals("Firstname is wrong", payload.getEvent().getFirstname(),
                latestRecord.value().getEvent().getFirstname());
        assertEquals("Lastname is wrong", payload.getEvent().getLastname(),
                latestRecord.value().getEvent().getLastname());
        assertEquals("Payload id is wrong", payload.getEvent().getId(), latestRecord.value().getEvent().getId());
        assertEquals("Relatives list is wrong", payload.getEvent().getRelatives(),
                latestRecord.value().getEvent().getRelatives());
        assertEquals("Status is wrong", payload.getEvent().getStatus(), latestRecord.value().getEvent().getStatus());
        if (payload.getSentAt().compareTo(latestRecord.value().getSentAt()) > 0) {
            throw new IllegalArgumentException(
                    "Sent at values of last message in queue is less than time of notification trigger: " +
                            payload.getSentAt() + " is less than " + latestRecord.value().getSentAt());
        }
    }

    protected House getHouse(int id, LocalDate lastUsed) {
        House house = new House();
        house.setId((long) id);
        house.setCity("Magdeburg");
        house.setNumber(1L);
        house.setStreet("Hohenwarsleben");
        house.setLastUsed(lastUsed);
        return house;
    }

    protected HousePayload getHousePayload(int id, House event, String eventType, LocalDateTime sentAt) {
        HousePayload housePayload = new HousePayload();
        housePayload.setEventId(id);
        housePayload.setEvent(event);
        housePayload.setEventType(eventType);
        housePayload.setSentAt(OffsetDateTime.from(sentAt));
        return housePayload;
    }

    protected PeoplePayload getPeoplePayload(int id, People event, LocalDateTime sentAt) {
        PeoplePayload payload = new PeoplePayload();
        payload.setEventId(id);
        payload.setEvent(event);
        payload.setEventType("update");
        payload.setSentAt(OffsetDateTime.from(sentAt));
        return payload;
    }

    protected People getPeople(int id, String firstname, String lastname, LocalDate birthday, Address address,
                               List<Long> children,  List<Long> relatives) {
        People people = new People();
        people.setId((long) id);
        people.setFirstname(firstname);
        people.setLastname(lastname);
        people.setBirthday(birthday);
        people.setStatus(People.StatusEnum.ALIVE);
        people.setAddressData(address);
        people.setChildren(children);
        people.setRelatives(relatives);
        return people;
    }

    protected Address getAddress(int regId, long[] homeIds) {
        Address address = new Address();
        address.setRegistrationId(regId);
        address.setHomeIds(homeIds);
        return address;
    }

    protected HousePayload getHouseProducerPayload(String eventType) {
        int key = OffsetDateTime.now().getNano();
        House house = getHouse(key, LocalDate.now());
        return getHousePayload(key, house, eventType, LocalDateTime.now());
    }

    protected void sendHouseProducerRecord(HousePayload payload, KafkaProducer<Integer, HousePayload> producerHouses) {
        ProducerRecord<Integer, HousePayload> record = new ProducerRecord<>(HOUSES_TOPIC, 0, System.currentTimeMillis(),
                payload.getEventId(), payload);
        producerHouses.send(record, (metadata, exception) -> LOGGER.info("Next message was send: topic: " + metadata.topic() + ", " + ", Timestamp: " +
                metadata.timestamp() + ", Partition: " + metadata.partition()));
        producerHouses.flush();
    }
}
