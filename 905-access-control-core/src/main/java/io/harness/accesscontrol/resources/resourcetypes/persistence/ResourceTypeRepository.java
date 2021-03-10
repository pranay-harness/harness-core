package io.harness.accesscontrol.resources.resourcetypes.persistence;

import io.harness.annotation.HarnessRepo;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface ResourceTypeRepository extends PagingAndSortingRepository<ResourceTypeDBO, String> {
  Optional<ResourceTypeDBO> findByIdentifier(String identifier);
  Optional<ResourceTypeDBO> findByPermissionKey(String permissionKey);
}
