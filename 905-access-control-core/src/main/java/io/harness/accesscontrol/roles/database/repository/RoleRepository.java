package io.harness.accesscontrol.roles.database.repository;

import io.harness.accesscontrol.roles.database.Role;
import io.harness.annotation.HarnessRepo;

import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@ValidateOnExecution
public interface RoleRepository extends PagingAndSortingRepository<Role, String>, RoleCustomRepository {
  Optional<Role> findByIdentifierAndParentIdentifier(@NotNull String identifier, String parentIdentifier);
  Role deleteByIdentifierAndParentIdentifier(@NotNull String identifier, String parentIdentifier);
}
