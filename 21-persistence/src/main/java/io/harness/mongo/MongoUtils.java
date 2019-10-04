package io.harness.mongo;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.beans.PageRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@UtilityClass
@Slf4j
public class MongoUtils {
  /**
   * Get count.
   *
   * @param <T>    the generic type
   * @param q      the q
   * @param mapper the mapper
   * @param cls    the cls
   * @param req    the req
   * @return the page response
   */
  public static <T> long getCount(Datastore datastore, Query<T> q, Mapper mapper, Class<T> cls, PageRequest<T> req) {
    q = PageController.applyPageRequest(datastore, q, req, cls, mapper);

    return q.count();
  }

  /**
   * Sets the unset.
   *
   * @param <T>   the generic type
   * @param ops   the ops
   * @param field the field
   * @param value the value
   * @return the update operations
   */
  public static <T> UpdateOperations<T> setUnset(UpdateOperations<T> ops, String field, Object value) {
    if (value == null || (value instanceof String && isBlank((String) value))) {
      return ops.unset(field);
    } else {
      return ops.set(field, value);
    }
  }
}
