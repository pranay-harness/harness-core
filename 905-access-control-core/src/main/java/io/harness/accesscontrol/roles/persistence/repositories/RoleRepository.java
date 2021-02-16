package io.harness.accesscontrol.roles.persistence.repositories;

import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.annotation.HarnessRepo;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@ValidateOnExecution
public interface RoleRepository extends PagingAndSortingRepository<RoleDBO, String>, RoleCustomRepository {
  Optional<RoleDBO> findByIdentifierAndScopeIdentifier(@NotNull String identifier, String parentIdentifier);
  List<RoleDBO> deleteByIdentifierAndScopeIdentifier(@NotNull String identifier, String parentIdentifier);
}
