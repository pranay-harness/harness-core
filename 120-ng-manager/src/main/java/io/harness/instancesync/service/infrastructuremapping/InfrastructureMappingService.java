package io.harness.instancesync.service.infrastructuremapping;

import io.harness.instancesync.dto.infrastructureMapping.InfrastructureMapping;

public interface InfrastructureMappingService {
  InfrastructureMapping get(String infrastructureMappingId);
}
