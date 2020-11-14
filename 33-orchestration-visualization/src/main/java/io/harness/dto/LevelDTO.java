package io.harness.dto;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LevelDTO {
  String setupId;
  String runtimeId;
  String identifier;
  String stepType;
  String group;
}
