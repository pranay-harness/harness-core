package io.harness.repositories.infrastructuremapping;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.infrastructureMapping.InfrastructureMapping;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface InfrastructureMappingRepository extends CrudRepository<InfrastructureMapping, String> {
  Optional<InfrastructureMapping> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String infrastructureMappingId);
}
