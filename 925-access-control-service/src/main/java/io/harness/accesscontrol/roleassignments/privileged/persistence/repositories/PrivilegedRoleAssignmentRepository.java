package io.harness.accesscontrol.roleassignments.privileged.persistence.repositories;

import io.harness.accesscontrol.roleassignments.privileged.persistence.PrivilegedRoleAssignmentDBO;
import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(HarnessTeam.PL)
@HarnessRepo
public interface PrivilegedRoleAssignmentRepository
    extends PagingAndSortingRepository<PrivilegedRoleAssignmentDBO, String>, PrivilegedRoleAssignmentCustomRepository {}
