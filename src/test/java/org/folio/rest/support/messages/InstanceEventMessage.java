package org.folio.rest.support.messages;

import static org.folio.kafka.KafkaHeaderUtils.kafkaHeadersToMap;

import java.util.Map;

import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InstanceEventMessage {
  public static InstanceEventMessage fromConsumerRecord(
    KafkaConsumerRecord<String, JsonObject> consumerRecord) {

    final var value = consumerRecord.value();

    return new InstanceEventMessage(
      value.getString("type"),
      value.getString("tenant"),
      value.getJsonObject("new"),
      value.getJsonObject("old"),
      kafkaHeadersToMap(consumerRecord.headers()));
  }

  private final String type;
  private final String tenant;
  private final JsonObject newRepresentation;
  private final JsonObject oldRepresentation;
  private final Map<String, String> headers;
}
