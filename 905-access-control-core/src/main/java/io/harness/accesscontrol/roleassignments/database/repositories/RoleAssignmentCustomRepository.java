package io.harness.accesscontrol.roleassignments.database.repositories;

import io.harness.accesscontrol.roleassignments.database.RoleAssignment;

import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface RoleAssignmentCustomRepository {
  Page<RoleAssignment> findAll(@NotNull Criteria criteria, @NotNull Pageable pageable);
}
