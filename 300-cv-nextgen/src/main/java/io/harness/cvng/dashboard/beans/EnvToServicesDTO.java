package io.harness.cvng.dashboard.beans;

import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;

import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class EnvToServicesDTO {
  EnvironmentResponseDTO environment;
  @Singular Set<ServiceResponseDTO> services;
}
