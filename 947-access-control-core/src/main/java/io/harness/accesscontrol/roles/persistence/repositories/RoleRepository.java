package io.harness.accesscontrol.roles.persistence.repositories;

import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(HarnessTeam.PL)
@HarnessRepo
@ValidateOnExecution
public interface RoleRepository extends PagingAndSortingRepository<RoleDBO, String>, RoleCustomRepository {
  List<RoleDBO> deleteByIdentifierAndScopeIdentifierAndManaged(
      @NotNull String identifier, String parentIdentifier, boolean managed);
}
