package io.harness.mongo;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@UtilityClass
@Slf4j
public class MongoUtils {
  public static <T> UpdateOperations<T> setUnset(UpdateOperations<T> ops, String field, Object value) {
    if (value == null || (value instanceof String && isBlank((String) value))) {
      return ops.unset(field);
    } else {
      return ops.set(field, value);
    }
  }

  public static <T> UpdateOperations<T> setUnsetOnInsert(UpdateOperations<T> ops, String field, Object value) {
    if (value == null || (value instanceof String && isBlank((String) value))) {
      return ops.unset(field);
    } else {
      return ops.setOnInsert(field, value);
    }
  }
}
