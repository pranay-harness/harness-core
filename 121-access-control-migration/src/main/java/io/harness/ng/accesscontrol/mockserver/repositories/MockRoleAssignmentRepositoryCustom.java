package io.harness.ng.accesscontrol.mockserver.repositories;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.mockserver.models.MockRoleAssignment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface MockRoleAssignmentRepositoryCustom {
  Page<MockRoleAssignment> findAll(Criteria criteria, Pageable pageable);
}
