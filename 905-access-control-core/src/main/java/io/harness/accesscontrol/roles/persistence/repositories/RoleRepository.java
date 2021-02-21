package io.harness.accesscontrol.roles.persistence.repositories;

import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.annotation.HarnessRepo;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@ValidateOnExecution
public interface RoleRepository extends PagingAndSortingRepository<RoleDBO, String>, RoleCustomRepository {
  List<RoleDBO> deleteByIdentifierAndScopeIdentifier(@NotNull String identifier, String parentIdentifier);
}
