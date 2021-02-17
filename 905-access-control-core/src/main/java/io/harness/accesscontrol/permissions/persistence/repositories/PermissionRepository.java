package io.harness.accesscontrol.permissions.persistence.repositories;

import io.harness.accesscontrol.permissions.persistence.PermissionDBO;
import io.harness.annotation.HarnessRepo;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface PermissionRepository
    extends PagingAndSortingRepository<PermissionDBO, String>, PermissionCustomRepository {
  Optional<PermissionDBO> findByIdentifier(String identifier);
  void deleteByIdentifier(String identifier);
}
