package io.harness.ng.core.environment.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnvironmentResponse {
  EnvironmentResponseDTO environment;
  Long createdAt;
  Long lastModifiedAt;

  @Builder
  public EnvironmentResponse(EnvironmentResponseDTO environment, Long createdAt, Long lastModifiedAt) {
    this.environment = environment;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
  }
}