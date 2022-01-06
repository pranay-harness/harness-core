/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Utils class for common operations
 * required by search entities
 *
 * @author utkarsh
 */

@OwnedBy(PL)
@UtilityClass
@Slf4j
public final class SearchEntityUtils {
  static String mergeSettings(String baseSettingsString, String entitySettingsString) {
    JsonObject entitySettings = new JsonParser().parse(entitySettingsString).getAsJsonObject();
    JsonObject baseSettings = new JsonParser().parse(baseSettingsString).getAsJsonObject();
    JsonObject temp = entitySettings.get("mappings").getAsJsonObject().get("properties").getAsJsonObject();

    Set<Entry<String, JsonElement>> entrySet = temp.entrySet();

    for (Map.Entry<String, JsonElement> entry : entrySet) {
      baseSettings.get("mappings")
          .getAsJsonObject()
          .get("properties")
          .getAsJsonObject()
          .add(entry.getKey(), temp.get(entry.getKey()));
    }
    return baseSettings.toString();
  }

  public static Optional<String> convertToJson(Object object) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    try {
      String jsonString = mapper.writeValueAsString(object);
      return Optional.of(jsonString);
    } catch (JsonProcessingException e) {
      log.error("Could not convert view to json", e);
      return Optional.empty();
    }
  }

  public static List<Long> truncateList(List<Long> list, long value) {
    if (list.isEmpty()) {
      return list;
    }
    Collections.reverse(list);
    int index = Collections.binarySearch(list, value, Collections.reverseOrder());
    index = Math.abs(index + 1);
    return list.subList(0, index);
  }

  public static long getTimestampNdaysBackInMillis(Integer daysToRetain) {
    return System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysToRetain.longValue());
  }

  public static Map<String, Object> convertToMap(Object object) {
    return new ObjectMapper().convertValue(object, new TypeReference<Map<String, Object>>() {});
  }
}
