package org.folio.rest.support;

import java.util.Collection;
import java.util.stream.Collectors;

import org.folio.dbschema.ObjectMapperTool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// is mod-inventory-storage used as an artifact dependency

// dead code
public final class CollectionUtil {
  private CollectionUtil() {}

  /**
   * Makes deep copy of the collection using serialization to/from json.
   *
   * @throws IllegalArgumentException - if can not serialize/deserialize json.
   */
  public static <T> Collection<T> deepCopy(Collection<T> collection, Class<T> type) {
    if (collection == null) {
      return null;
    }

    return collection.stream()
      .map(record -> clone(record, type))
      .collect(Collectors.toList());
  }

  public static <T> T getFirst(Collection<T> collection) {
    return collection != null && collection.size() > 0
      ? collection.iterator().next() : null;
  }

  /**
   * @throws IllegalArgumentException - if can not serialize/deserialize to/from json
   */
  private static <T> T clone(T obj, Class<T> type) {
    try {
      final ObjectMapper jsonMapper = ObjectMapperTool.getMapper();
      return jsonMapper.readValue(jsonMapper.writeValueAsString(obj), type);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException(ex);
    }
  }
}
