package io.harness.service.infrastructuremapping;

import io.harness.dtos.InfrastructureMappingDTO;

import java.util.Optional;

@io.harness.annotations.dev.OwnedBy(io.harness.annotations.dev.HarnessTeam.DX)
public interface InfrastructureMappingService {
  Optional<InfrastructureMappingDTO> getByInfrastructureMappingId(String infrastructureMappingId);
}
