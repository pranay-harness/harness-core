/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.OrchestrationMap;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
@Slf4j
public class OrchestrationMapBackwardCompatibilityUtils {
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
