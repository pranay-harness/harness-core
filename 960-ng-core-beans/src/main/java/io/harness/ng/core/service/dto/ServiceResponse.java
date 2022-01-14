package io.harness.ng.core.service.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceResponse {
  ServiceResponseDTO service;
  Long createdAt;
  Long lastModifiedAt;

  @Builder
  public ServiceResponse(ServiceResponseDTO service, Long createdAt, Long lastModifiedAt) {
    this.service = service;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
  }
}