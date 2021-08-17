package io.harness.accesscontrol.roleassignments.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.accesscontrol.roleassignments.privileged.AdminPrivilegedRoleAssignmentMapper;
import io.harness.accesscontrol.roleassignments.privileged.PrivilegedRoleAssignmentHandler;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@OwnedBy(PL)
public class PrivilegedRoleAssignmentMigration implements NGMigration {
  private final RoleAssignmentRepository roleAssignmentRepository;
  private final PrivilegedRoleAssignmentHandler privilegedRoleAssignmentHandler;

  @Inject
  public PrivilegedRoleAssignmentMigration(RoleAssignmentRepository roleAssignmentRepository,
      PrivilegedRoleAssignmentHandler privilegedRoleAssignmentHandler) {
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.privilegedRoleAssignmentHandler = privilegedRoleAssignmentHandler;
  }

  @Override
  public void migrate() {
    int pageSize = 1000;
    int pageIndex = 0;
    Pageable pageable = PageRequest.of(pageIndex, pageSize);
    Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.roleIdentifier)
                            .in(AdminPrivilegedRoleAssignmentMapper.roleToPrivilegedRole.keySet())
                            .and(RoleAssignmentDBOKeys.resourceGroupIdentifier)
                            .in(AdminPrivilegedRoleAssignmentMapper.ALL_RESOURCES_RESOURCE_GROUP);
    long totalPages = roleAssignmentRepository.findAll(criteria, pageable).getTotalPages();
    do {
      pageable = PageRequest.of(pageIndex, pageSize);
      List<RoleAssignmentDBO> roleAssignmentList = roleAssignmentRepository.findAll(criteria, pageable).getContent();
      if (isEmpty(roleAssignmentList)) {
        return;
      }
      for (RoleAssignmentDBO roleAssignment : roleAssignmentList) {
        privilegedRoleAssignmentHandler.handleRoleAssignmentCreate(roleAssignment);
      }
      pageIndex++;
    } while (pageIndex < totalPages);
  }
}
