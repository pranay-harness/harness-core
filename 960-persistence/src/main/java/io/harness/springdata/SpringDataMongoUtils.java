package io.harness.springdata;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@UtilityClass
@Slf4j
public class SpringDataMongoUtils {
  public static final FindAndModifyOptions returnNewOptions = new FindAndModifyOptions().returnNew(true).upsert(false);

  public static Update setUnset(Update ops, String field, Object value) {
    if (value == null || (value instanceof String && isBlank((String) value))) {
      return ops.unset(field);
    } else {
      return ops.set(field, value);
    }
  }

  public static Update setUnsetOnInsert(Update ops, String field, Object value) {
    if (value == null || (value instanceof String && isBlank((String) value))) {
      return ops.unset(field);
    } else {
      return ops.setOnInsert(field, value);
    }
  }

  public void populateInFilter(Criteria criteria, String fieldName, List<?> values) {
    if (isNotEmpty(values)) {
      criteria.and(fieldName).in(values);
    }
  }
}
