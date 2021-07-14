package io.harness.service.infrastructuremapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.entities.InfrastructureMapping;
import io.harness.mappers.InfrastructureMappingMapper;
import io.harness.repositories.infrastructuremapping.InfrastructureMappingRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;

@io.harness.annotations.dev.OwnedBy(HarnessTeam.DX)
@Singleton
public class InfrastructureMappingServiceImpl implements InfrastructureMappingService {
  @Inject InfrastructureMappingRepository infrastructureMappingRepository;

  public Optional<InfrastructureMappingDTO> getByInfrastructureMappingId(String infrastructureMappingId) {
    Optional<InfrastructureMapping> infrastructureMappingOptional =
        infrastructureMappingRepository.findById(infrastructureMappingId);
    return infrastructureMappingOptional.map(InfrastructureMappingMapper::toDTO);
  }
}
