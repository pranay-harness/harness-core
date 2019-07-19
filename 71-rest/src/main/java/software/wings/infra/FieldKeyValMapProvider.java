package software.wings.infra;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.annotation.IncludeInFieldMap;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public interface FieldKeyValMapProvider {
  default Map<String, Object> getFieldMapForClass() {
    Class cls = this.getClass();
    Map<String, Object> queryMap = new HashMap<>();
    Field[] fields = cls.getDeclaredFields();
    for (Field field : fields) {
      if (field.isAnnotationPresent(IncludeInFieldMap.class)) {
        IncludeInFieldMap includeInFieldMap = field.getAnnotation(IncludeInFieldMap.class);
        try {
          field.setAccessible(true);
          if (isBlank(includeInFieldMap.key())) {
            queryMap.put(field.getName(), field.get(this));
          } else {
            queryMap.put(includeInFieldMap.key(), field.get(this));
          }
        } catch (IllegalAccessException e) {
          throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
        }
      }
    }
    return queryMap;
  }
}
