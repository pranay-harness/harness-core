package io.harness.cvng.dashboard.beans;

import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class EnvToServicesDTO {
  EnvironmentResponseDTO environment;
  @Singular Set<ServiceResponseDTO> services;
}
