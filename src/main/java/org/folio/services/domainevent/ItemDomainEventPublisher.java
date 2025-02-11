package org.folio.services.domainevent;

import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.toList;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.folio.InventoryKafkaTopic.ITEM;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import io.vertx.core.Context;
import io.vertx.core.Future;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.Logger;
import org.folio.persist.HoldingsRepository;
import org.folio.persist.ItemRepository;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Item;

public class ItemDomainEventPublisher extends AbstractDomainEventPublisher<Item, ItemWithInstanceId> {
  private static final Logger log = getLogger(ItemDomainEventPublisher.class);

  private final HoldingsRepository holdingsRepository;

  public ItemDomainEventPublisher(Context context, Map<String, String> okapiHeaders) {
    super(new ItemRepository(context, okapiHeaders),
      new CommonDomainEventPublisher<>(context, okapiHeaders,
        ITEM.fullTopicName(tenantId(okapiHeaders))));

    holdingsRepository = new HoldingsRepository(context, okapiHeaders);
  }

  public Future<Void> publishUpdated(Item newItem, Item oldItem, HoldingsRecord newHoldings,
                                     HoldingsRecord oldHoldings) {
    ItemWithInstanceId oldItemWithId = new ItemWithInstanceId(oldItem, oldHoldings.getInstanceId());
    ItemWithInstanceId newItemWithId = new ItemWithInstanceId(newItem, newHoldings.getInstanceId());

    return domainEventService.publishRecordUpdated(newHoldings.getInstanceId(), oldItemWithId, newItemWithId);
  }

  public Future<Void> publishUpdated(HoldingsRecord oldHoldings, HoldingsRecord newHoldings, List<Item> oldItems) {
    if (oldItems.isEmpty()) {
      log.info("No items were updated, skipping event sending");
      return succeededFuture();
    }

    log.info("[{}] items were updated, sending events for them", oldItems.size());

    return repository.getById(oldItems, Item::getId)
      .map(updatedItems -> mapOldItemsToNew(oldHoldings, newHoldings, oldItems, updatedItems.values()))
      .compose(domainEventService::publishRecordsUpdated);
  }

  @Override
  public void publishRemoved(String instanceId, String itemRaw) {
    String instanceIdAndItemRaw = "{\"instanceId\":\"" + instanceId + "\"," + itemRaw.substring(1);
    domainEventService.publishRecordRemoved(instanceId, instanceIdAndItemRaw);
  }

  @Override
  protected Future<List<Pair<String, Item>>> getInstanceIds(Collection<Item> items) {
    return holdingsRepository.getById(items, Item::getHoldingsRecordId)
      .map(holdings -> items.stream()
        .map(item -> pair(getInstanceId(holdings, item), item))
        .collect(toList()));
  }

  @Override
  protected ItemWithInstanceId convertDomainToEvent(String instanceId, Item item) {
    return new ItemWithInstanceId(item, instanceId);
  }

  @Override
  protected String getId(Item item) {
    return item.getId();
  }

  private List<Triple<String, ItemWithInstanceId, ItemWithInstanceId>> mapOldItemsToNew(
    HoldingsRecord oldHoldings, HoldingsRecord newHoldings, Collection<Item> oldItems, Collection<Item> newItems) {

    return mapOldRecordsToNew(
      oldItems.stream().map(item -> pair(oldHoldings.getInstanceId(), item)).collect(toList()),
      newItems.stream().map(item -> pair(newHoldings.getInstanceId(), item)).collect(toList()));
  }

  private String getInstanceId(Map<String, HoldingsRecord> holdings, Item item) {
    return holdings.get(item.getHoldingsRecordId()).getInstanceId();
  }
}
