package io.harness.accesscontrol.resources.resourcetypes.persistence;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(HarnessTeam.PL)
@HarnessRepo
public interface ResourceTypeRepository extends PagingAndSortingRepository<ResourceTypeDBO, String> {
  Optional<ResourceTypeDBO> findByIdentifier(String identifier);
  Optional<ResourceTypeDBO> findByPermissionKey(String permissionKey);
}
