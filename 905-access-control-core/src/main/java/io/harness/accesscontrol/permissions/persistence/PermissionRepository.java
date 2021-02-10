package io.harness.accesscontrol.permissions.persistence;

import io.harness.annotation.HarnessRepo;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface PermissionRepository extends PagingAndSortingRepository<PermissionDBO, String> {
  Optional<PermissionDBO> findByIdentifier(String identifier);
  void deleteByIdentifier(String identifier);
  Collection<PermissionDBO> findAllByScopesContaining(String scope);
}
