package io.harness.repositories.mockroleassignmentserver.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.mockserver.MockRoleAssignment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface MockRoleAssignmentRepositoryCustom {
  Page<MockRoleAssignment> findAll(Criteria criteria, Pageable pageable);
}
