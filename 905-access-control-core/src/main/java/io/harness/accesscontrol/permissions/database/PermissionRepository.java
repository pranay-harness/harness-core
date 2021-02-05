package io.harness.accesscontrol.permissions.database;

import io.harness.annotation.HarnessRepo;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface PermissionRepository extends PagingAndSortingRepository<Permission, String> {
  Optional<Permission> findByIdentifier(String identifier);
  void deleteByIdentifier(String identifier);
  Collection<Permission> findAllByScopesContaining(String scope);
  Collection<Permission> findAllByScopesContainingAndResourceType(String scope, String resourceType);
}
