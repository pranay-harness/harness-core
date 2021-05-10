package io.harness.repository.infrastructuremapping;

import io.harness.dto.infrastructureMapping.InfrastructureMapping;

public interface InfrastructureMappingRepository {
  InfrastructureMapping get(String accountId, String orgId, String projectId, String infrastructureMappingId);

  void save(InfrastructureMapping infrastructureMapping);
}
