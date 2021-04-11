package io.harness.accesscontrol.roleassignments.persistence.repositories;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.PL)
public interface RoleAssignmentCustomRepository {
  Page<RoleAssignmentDBO> findAll(@NotNull Criteria criteria, @NotNull Pageable pageable);

  long deleteMulti(@NotNull Criteria criteria);

  List<RoleAssignmentDBO> insertAllIgnoringDuplicates(List<RoleAssignmentDBO> collect);
}
