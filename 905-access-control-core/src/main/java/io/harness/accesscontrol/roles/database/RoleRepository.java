package io.harness.accesscontrol.roles.database;

import io.harness.annotation.HarnessRepo;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface RoleRepository extends PagingAndSortingRepository<Role, String> {
  Optional<Role> findByIdentifierAndParentIdentifier(String identifier, String parentIdentifier);
  void deleteByIdentifierAndParentIdentifier(String identifier, String parentIdentifier);
}
