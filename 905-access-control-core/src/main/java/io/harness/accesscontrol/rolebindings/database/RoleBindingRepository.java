package io.harness.accesscontrol.rolebindings.database;

import io.harness.annotation.HarnessRepo;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface RoleBindingRepository extends PagingAndSortingRepository<RoleBinding, String> {
  Optional<RoleBinding> findByIdentifierAndParentIdentifier(String identifier, String parentIdentifier);
  void deleteByIdentifierAndParentIdentifier(String identifier, String parentIdentifier);
}