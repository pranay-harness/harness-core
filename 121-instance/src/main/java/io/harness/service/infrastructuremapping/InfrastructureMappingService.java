package io.harness.service.infrastructuremapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.InfrastructureMappingDTO;

import java.util.Optional;

@OwnedBy(HarnessTeam.DX)
public interface InfrastructureMappingService {
  Optional<InfrastructureMappingDTO> getByInfrastructureMappingId(String infrastructureMappingId);

  Optional<InfrastructureMappingDTO> createNewOrReturnExistingInfrastructureMapping(
      InfrastructureMappingDTO infrastructureMappingDTO);
}
