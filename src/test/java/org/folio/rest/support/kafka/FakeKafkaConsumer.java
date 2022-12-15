package org.folio.rest.support.kafka;

import static io.vertx.kafka.client.consumer.KafkaConsumer.create;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.services.KafkaEnvironmentProperties;
import org.folio.rest.support.messages.EventMessage;
import org.joda.time.DateTime;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.serialization.JsonObjectDeserializer;

public final class FakeKafkaConsumer {
  private static final Logger logger = LogManager.getLogger();

  private final static Map<String, List<KafkaConsumerRecord<String, JsonObject>>> itemEvents =
    new ConcurrentHashMap<>();
  private final static Map<String, List<KafkaConsumerRecord<String, JsonObject>>> instanceEvents =
    new ConcurrentHashMap<>();
  private final static Map<String, List<KafkaConsumerRecord<String, JsonObject>>> holdingsEvents =
    new ConcurrentHashMap<>();
  private final static Map<String, List<KafkaConsumerRecord<String, JsonObject>>> authorityEvents =
    new ConcurrentHashMap<>();
  private final static Map<String, List<KafkaConsumerRecord<String, JsonObject>>> boundWithEvents =
    new ConcurrentHashMap<>();

  // Provide a strong reference to reduce the chances of deallocation before
  // all clients are properly unsubscribed.
  private KafkaConsumer<String, JsonObject> consumer;

  private static DateTime timestamp = DateTime.now();

  public FakeKafkaConsumer consume(Vertx vertx) {
    final KafkaConsumer<String, JsonObject> consumer = create(vertx, consumerProperties());

    // These definitions are deliberately separate to the production definitions
    // This is so these can be changed independently to demonstrate
    // tests failing for the right reason prior to changing the production code
    final var INSTANCE_TOPIC_NAME = "folio.test_tenant.inventory.instance";
    final var HOLDINGS_TOPIC_NAME = "folio.test_tenant.inventory.holdings-record";
    final var ITEM_TOPIC_NAME = "folio.test_tenant.inventory.item";
    final var AUTHORITY_TOPIC_NAME = "folio.test_tenant.inventory.authority";
    final var BOUND_TOPIC_NAME = "folio.test_tenant.inventory.bound-with";

    consumer.subscribe(Set.of(INSTANCE_TOPIC_NAME, HOLDINGS_TOPIC_NAME, ITEM_TOPIC_NAME, AUTHORITY_TOPIC_NAME, BOUND_TOPIC_NAME));

    consumer.handler(message -> {
      final List<KafkaConsumerRecord<String, JsonObject>> storageList;

      // Messages that are earlier than the timestamp are stale and should be ignored.
      if (message.timestamp() < timestamp.toInstant().getMillis()) {
        logger.debug("Timestamp is " + message.timestamp()
          + " less than " + timestamp.toInstant().getMillis()
          + " for message: " + message.topic() + ", " + message.key() + ", "
          + message.value().encodePrettily());
        return;
      }

      switch (message.topic()) {
        case ITEM_TOPIC_NAME:
          storageList = itemEvents.computeIfAbsent(instanceAndIdKey(message),
            k -> new ArrayList<>());
          break;
        case INSTANCE_TOPIC_NAME:
          storageList = instanceEvents.computeIfAbsent(message.key(),
            k -> new ArrayList<>());
          break;
        case HOLDINGS_TOPIC_NAME:
          storageList = holdingsEvents.computeIfAbsent(instanceAndIdKey(message),
            k -> new ArrayList<>());
          break;
        case AUTHORITY_TOPIC_NAME:
          storageList = authorityEvents.computeIfAbsent(message.key(),
            k -> new ArrayList<>());
          break;
        case BOUND_TOPIC_NAME:
          storageList = boundWithEvents.computeIfAbsent(message.key(),
            k -> new ArrayList<>());

          logger.info("Bound With Message Received: {}: {}", message.key(), message.value().encodePrettily());

          break;
        default:
          throw new IllegalArgumentException("Undefined topic");
      }

      storageList.add(message);
    });

    timestamp = DateTime.now();

    // Assign the created consumer to the class being returned.
    // The caller of this function may then be able to call unsubscribe()
    // as needed. This ensures that the reference remains strong once
    // this once out of the scope of this function.
    this.setConsumer(consumer);

    return this;
  }

  public void resetTimestamp() {
    // Update the timestamp to designate ignoring stale messages.
    timestamp = DateTime.now();
  }

  public void unsubscribe() {
    consumer.unsubscribe();
  }

  public static void clearAllEvents() {
    itemEvents.clear();
    instanceEvents.clear();
    holdingsEvents.clear();
    authorityEvents.clear();
    boundWithEvents.clear();
  }

  public static Collection<JsonObject> getAllPublishedBoundWithEvents() {
    List<JsonObject> list = new ArrayList<>();
    boundWithEvents.values().forEach(collection -> collection.forEach(record -> list.add(record.value())));
    return list;
  }

  public static int getAllPublishedAuthoritiesCount() {
    return authorityEvents.size();
  }

  public static Collection<EventMessage> getMessagesForAuthority(String authorityId) {
    return getEmptyDefault(authorityEvents, authorityId)
      .stream()
      .map(EventMessage::fromConsumerRecord)
      .collect(Collectors.toList());
  }

  public static int getAllPublishedInstanceIdsCount() {
    return instanceEvents.size();
  }

  public static Collection<EventMessage> getMessagesForInstance(String instanceId) {
    return getEmptyDefault(instanceEvents, instanceId)
      .stream()
      .map(EventMessage::fromConsumerRecord)
      .collect(Collectors.toList());
  }

  public static Collection<EventMessage> getMessagesForInstances(List<String> instanceIds) {
    return instanceIds.stream()
      .map(FakeKafkaConsumer::getMessagesForInstance)
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  public static Collection<EventMessage> getMessagesForHoldings(
    String instanceId, String holdingsId) {

    return getEmptyDefault(holdingsEvents, instanceAndIdKey(instanceId, holdingsId))
      .stream()
      .map(EventMessage::fromConsumerRecord)
      .collect(Collectors.toList());
  }

  public static Collection<KafkaConsumerRecord<String, JsonObject> > getItemEvents(
    String instanceId, String itemId) {

    return getEmptyDefault(itemEvents, instanceAndIdKey(instanceId, itemId));
  }

  public static Collection<EventMessage> getMessagesForItem(
    String instanceId, String itemId) {

    return getItemEvents(instanceId, itemId)
      .stream()
      .map(EventMessage::fromConsumerRecord)
      .collect(Collectors.toList());
  }

  public static Collection<EventMessage> getMessagesForBoundWith(String instanceId) {
    return getEmptyDefault(boundWithEvents, instanceId)
      .stream()
      .map(EventMessage::fromConsumerRecord)
      .collect(Collectors.toList());
  }

  private static <T> List<T> getEmptyDefault(Map<String, List<T>> map, String key) {
    return map.getOrDefault(key, emptyList());
  }

  private static String instanceAndIdKey(String instanceId, String itemId) {
    return instanceId + "_" + itemId;
  }

  private static String instanceAndIdKey(KafkaConsumerRecord<String, JsonObject> message) {
    final JsonObject payload = message.value();
    final var oldOrNew = payload.containsKey("new")
      ? payload.getJsonObject("new") : payload.getJsonObject("old");

    final var id = oldOrNew != null ? oldOrNew.getString("id") : null;

    return instanceAndIdKey(message.key(), id);
  }

  private void setConsumer(KafkaConsumer<String, JsonObject> consumer) {
    this.consumer = consumer;
  }

  private Map<String, String> consumerProperties() {
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", KafkaEnvironmentProperties.host() + ":" + KafkaEnvironmentProperties.port());
    config.put("key.deserializer", StringDeserializer.class.getName());
    config.put("value.deserializer", JsonObjectDeserializer.class.getName());
    config.put("group.id", "folio_test");
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", "false");

    return config;
  }
}
