package io.harness.accesscontrol.roleassignments.persistence.repositories;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;

import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface RoleAssignmentCustomRepository {
  Page<RoleAssignmentDBO> findAll(@NotNull Criteria criteria, @NotNull Pageable pageable);

  long deleteMulti(@NotNull Criteria criteria);
}
