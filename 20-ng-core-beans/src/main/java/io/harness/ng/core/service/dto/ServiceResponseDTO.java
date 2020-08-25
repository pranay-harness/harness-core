package io.harness.ng.core.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceResponseDTO {
  String accountId;
  String identifier;
  String orgIdentifier;
  String projectIdentifier;
  String name;
  String description;
  boolean deleted;
}
