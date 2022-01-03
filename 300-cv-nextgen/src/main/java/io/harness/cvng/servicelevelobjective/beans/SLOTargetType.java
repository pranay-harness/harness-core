package io.harness.cvng.servicelevelobjective.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;

@AllArgsConstructor
public enum SLOTargetType {
  @JsonProperty("Rolling") ROLLING("Rolling"),
  @JsonProperty("Calender") CALENDER("Calender");

  private static Map<String, SLOTargetType> STRING_TO_TYPE_MAP;

  @Getter private String identifier;

  public static SLOTargetType fromString(String stringValue) {
    if (MapUtils.isEmpty(STRING_TO_TYPE_MAP)) {
      STRING_TO_TYPE_MAP = Arrays.stream(SLOTargetType.values())
                               .collect(Collectors.toMap(SLOTargetType::getIdentifier, Function.identity()));
    }
    if (!STRING_TO_TYPE_MAP.containsKey(stringValue)) {
      throw new IllegalArgumentException("SLOTargetType should be in : " + STRING_TO_TYPE_MAP.keySet());
    }
    return STRING_TO_TYPE_MAP.get(stringValue);
  }
}
