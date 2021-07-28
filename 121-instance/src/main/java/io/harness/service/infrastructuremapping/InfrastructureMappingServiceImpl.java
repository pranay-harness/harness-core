package io.harness.service.infrastructuremapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.entities.InfrastructureMapping;
import io.harness.mappers.InfrastructureMappingMapper;
import io.harness.repositories.infrastructuremapping.InfrastructureMappingRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

@OwnedBy(HarnessTeam.DX)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InfrastructureMappingServiceImpl implements InfrastructureMappingService {
  @Inject InfrastructureMappingRepository infrastructureMappingRepository;

  public Optional<InfrastructureMappingDTO> getByInfrastructureMappingId(String infrastructureMappingId) {
    Optional<InfrastructureMapping> infrastructureMappingOptional =
        infrastructureMappingRepository.findById(infrastructureMappingId);
    return infrastructureMappingOptional.map(InfrastructureMappingMapper::toDTO);
  }

  @Override
  public Optional<InfrastructureMappingDTO> createNewOrReturnExistingInfrastructureMapping(
      InfrastructureMappingDTO infrastructureMappingDTO) {
    try {
      InfrastructureMapping infrastructureMapping =
          infrastructureMappingRepository.save(InfrastructureMappingMapper.toEntity(infrastructureMappingDTO));
      return Optional.of(InfrastructureMappingMapper.toDTO(infrastructureMapping));
    } catch (DuplicateKeyException duplicateKeyException) {
      log.warn("Duplicate key error while inserting infrastructure mapping for infrastructure key : {}",
          infrastructureMappingDTO.getInfrastructureKey());
      Optional<InfrastructureMapping> infrastructureMappingOptional =
          infrastructureMappingRepository.findByInfrastructureKey(infrastructureMappingDTO.getInfrastructureKey());
      return infrastructureMappingOptional.map(InfrastructureMappingMapper::toDTO);
    }
  }
}
