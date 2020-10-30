package io.harness.ng.core.environment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.ng.core.environment.beans.EnvironmentType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentResponseDTO {
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String identifier;
  String name;
  String description;
  EnvironmentType type;
  boolean deleted;
  Map<String, String> tags;
  @JsonIgnore Long version;
}
