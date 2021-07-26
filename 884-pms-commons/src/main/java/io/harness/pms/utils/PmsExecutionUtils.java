package io.harness.pms.utils;

import io.harness.pms.data.OrchestrationMap;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@UtilityClass
@Slf4j
public class PmsExecutionUtils {
  /**
   * This method is used for backward compatibility
   * @param object can be of type {@link Document} (before)
   *                      and {@link java.util.LinkedHashMap} (current)
   * @return document representation of step parameters
   */
  public OrchestrationMap extractToOrchestrationMap(Object object) {
    if (object == null) {
      return OrchestrationMap.parse("{}");
    }
    if (object instanceof Document) {
      return OrchestrationMap.parse(((Document) object).toJson());
    } else if (object instanceof Map) {
      return OrchestrationMap.parse((Map) object);
    } else {
      throw new IllegalStateException(String.format("Unable to parse %s", object.getClass()));
    }
  }

  /**
   * This method is used for backward compatibility
   * @param map can have values be of type {@link Document} (before)
   *                      and {@link java.util.LinkedHashMap} (current)
   * @return document representation of step parameters
   */
  public Map<String, OrchestrationMap> convertToOrchestrationMap(Map<String, ? extends Map<String, Object>> map) {
    if (map == null) {
      return null;
    }
    return map.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, e -> extractToOrchestrationMap(e.getValue())));
  }
}
