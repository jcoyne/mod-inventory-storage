package org.folio.services.domainevent;

import static org.apache.logging.log4j.LogManager.getLogger;
import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.services.domainevent.DomainEvent.createEvent;
import static org.folio.services.domainevent.DomainEvent.deleteEvent;
import static org.folio.services.domainevent.DomainEvent.updateEvent;
import static org.folio.services.kafka.KafkaProducerServiceFactory.getKafkaProducerService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.Logger;
import org.folio.services.kafka.topic.KafkaTopic;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;

class BaseDomainEventService<T> {
  private static final Logger log = getLogger(BaseDomainEventService.class);

  private final Context vertxContext;
  private final Map<String, String> okapiHeaders;
  private final KafkaTopic kafkaTopic;

  BaseDomainEventService(Context vertxContext, Map<String, String> okapiHeaders,
    KafkaTopic kafkaTopic) {

    this.vertxContext = vertxContext;
    this.okapiHeaders = okapiHeaders;
    this.kafkaTopic = kafkaTopic;
  }

  Future<Void> recordUpdated(String instanceId, T oldRecord, T newRecord) {
    final DomainEvent<T> domainEvent = updateEvent(oldRecord, newRecord, getTenant());

    return sendMessage(instanceId, domainEvent);
  }

  Future<Void> recordCreated(String instanceId, T newRecord) {
    final DomainEvent<T> domainEvent = createEvent(newRecord, getTenant());

    return sendMessage(instanceId, domainEvent);
  }

  Future<Void> recordRemoved(String instanceId, T oldEntity) {
    final DomainEvent<T> domainEvent = deleteEvent(oldEntity, getTenant());

    return sendMessage(instanceId, domainEvent);
  }

  Future<Void> recordsRemoved(List<Pair<String, T>> records) {
    return CompositeFuture.all(records.stream()
      .map(record -> recordRemoved(record.getKey(), record.getValue()))
      .collect(Collectors.toList()))
      .map(notUsed -> null);
  }

  private Future<Void> sendMessage(String instanceId, DomainEvent<?> domainEvent) {
    log.debug("Sending domain event [{}], payload [{}]",
      instanceId, domainEvent);

    return getKafkaProducerService(vertxContext.owner())
      .sendMessage(instanceId, domainEvent, kafkaTopic)
      .onComplete(result -> {
        if (result.failed()) {
          log.error("Unable to send domain event [{}], payload - [{}]",
            instanceId, domainEvent, result.cause());
        }
      });
  }

  private String getTenant() {
    return tenantId(okapiHeaders);
  }
}
