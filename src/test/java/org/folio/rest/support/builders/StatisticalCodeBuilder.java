package org.folio.rest.support.builders;

import java.util.UUID;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.With;

@With
@AllArgsConstructor
@ToString
public final class StatisticalCodeBuilder extends JsonRequestBuilder implements Builder {
  private final String code;
  private final String name;
  private final UUID statisticalCodeTypeId;
  private final String source;

  public StatisticalCodeBuilder() {
    this(null, null, null, "test");
  }

  @Override
  public JsonObject create() {
    final var json = new JsonObject();

    put(json, "code", code);
    put(json, "name", name);
    put(json, "statisticalCodeTypeId", statisticalCodeTypeId);
    put(json, "source", source);

    return json;
  }
}
