package io.harness.ng.core.environment.dto;

import io.harness.ng.core.environment.beans.EnvironmentType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "EnvironmentResponse", description = "This is the Environment entity defined in Harness")
public class EnvironmentResponseDTO {
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String identifier;
  String name;
  String description;
  String color;
  EnvironmentType type;
  boolean deleted;
  Map<String, String> tags;
  @JsonIgnore Long version;
}
