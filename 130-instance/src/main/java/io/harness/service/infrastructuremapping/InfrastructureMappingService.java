package io.harness.service.infrastructuremapping;

import io.harness.dto.infrastructureMapping.InfrastructureMapping;

public interface InfrastructureMappingService {
  InfrastructureMapping get(String infrastructureMappingId);
}
