package io.harness.ng.core.environment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.ng.core.environment.beans.EnvironmentType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

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
  EnvironmentType type;
}
