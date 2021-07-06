package io.harness.accesscontrol.roleassignments.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.springframework.data.mongodb.core.query.Update.update;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@OwnedBy(PL)
public class RoleAssignmentScopeAdditionMigration implements NGMigration {
  private final RoleAssignmentRepository roleAssignmentRepository;
  private final ScopeService scopeService;

  @Inject
  public RoleAssignmentScopeAdditionMigration(
      RoleAssignmentRepository roleAssignmentRepository, ScopeService scopeService) {
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.scopeService = scopeService;
  }

  @Override
  public void migrate() {
    int pageSize = 1000;
    int pageIndex = 0;
    long totalPages;
    do {
      Pageable pageable = PageRequest.of(pageIndex, pageSize);
      Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.scopeLevel).exists(false);
      Page<RoleAssignmentDBO> roleAssignmentDBOPage = roleAssignmentRepository.findAll(criteria, pageable);
      pageIndex++;
      totalPages = roleAssignmentDBOPage.getTotalPages();

      List<RoleAssignmentDBO> roleAssignmentList = roleAssignmentDBOPage.getContent();
      if (isEmpty(roleAssignmentList)) {
        return;
      }
      for (RoleAssignmentDBO roleAssignment : roleAssignmentList) {
        Scope scope = scopeService.buildScopeFromScopeIdentifier(roleAssignment.getScopeIdentifier());
        roleAssignmentRepository.updateById(
            roleAssignment.getId(), update(RoleAssignmentDBOKeys.scopeLevel, scope.getLevel().toString()));
      }
    } while (pageIndex < totalPages);
  }
}
