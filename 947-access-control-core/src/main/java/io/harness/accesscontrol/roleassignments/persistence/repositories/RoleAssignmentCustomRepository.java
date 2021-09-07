package io.harness.accesscontrol.roleassignments.persistence.repositories;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.PL)
public interface RoleAssignmentCustomRepository {
  Page<RoleAssignmentDBO> findAll(@NotNull Criteria criteria, @NotNull Pageable pageable);

  boolean updateById(@NotEmpty String id, @NotNull Update updateOperation);

  long deleteMulti(@NotNull Criteria criteria);
}
