package io.harness.accesscontrol.roleassignments.migration;

import static io.harness.NGConstants.DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.NGConstants.DEFAULT_ORGANIZATION_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.NGConstants.DEFAULT_PROJECT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofMinutes;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.harness.HarnessScopeLevel;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@Singleton
@OwnedBy(PL)
public class RoleAssignmentResourceGroupMigration implements NGMigration {
  private final RoleAssignmentRepository roleAssignmentRepository;
  private static final String ALL_RESOURCES_RESOURCE_GROUP_IDENTIFIER = "_all_resources";

  @Inject
  public RoleAssignmentResourceGroupMigration(RoleAssignmentRepository roleAssignmentRepository) {
    this.roleAssignmentRepository = roleAssignmentRepository;
  }

  @Override
  public void migrate() {
    migrateInternal(HarnessScopeLevel.ACCOUNT);
    migrateInternal(HarnessScopeLevel.ORGANIZATION);
    migrateInternal(HarnessScopeLevel.PROJECT);
  }

  private void migrateInternal(ScopeLevel scopeLevel) {
    int pageSize = 1000;
    int pageIndex = 0;
    Pageable pageable = PageRequest.of(pageIndex, pageSize);
    Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.scopeLevel)
                            .is(scopeLevel.toString())
                            .and(RoleAssignmentDBOKeys.resourceGroupIdentifier)
                            .is(ALL_RESOURCES_RESOURCE_GROUP_IDENTIFIER);
    do {
      List<RoleAssignmentDBO> roleAssignmentList = roleAssignmentRepository.findAll(criteria, pageable).getContent();
      if (isEmpty(roleAssignmentList)) {
        return;
      }
      for (RoleAssignmentDBO roleAssignment : roleAssignmentList) {
        roleAssignmentRepository.save(buildRoleAssignmentDBO(scopeLevel, roleAssignment));
        roleAssignmentRepository.deleteById(roleAssignment.getId());
      }
      sleep(ofMinutes(2));
    } while (true);
  }

  private RoleAssignmentDBO buildRoleAssignmentDBO(ScopeLevel scopeLevel, RoleAssignmentDBO roleAssignmentDBO) {
    return RoleAssignmentDBO.builder()
        .identifier(roleAssignmentDBO.getIdentifier())
        .scopeIdentifier(roleAssignmentDBO.getScopeIdentifier())
        .scopeLevel(roleAssignmentDBO.getScopeLevel())
        .disabled(roleAssignmentDBO.isDisabled())
        .managed(roleAssignmentDBO.isManaged())
        .roleIdentifier(roleAssignmentDBO.getRoleIdentifier())
        .resourceGroupIdentifier(getResourceGroupIdentifier(scopeLevel))
        .principalIdentifier(roleAssignmentDBO.getPrincipalIdentifier())
        .principalType(roleAssignmentDBO.getPrincipalType())
        .createdAt(roleAssignmentDBO.getCreatedAt())
        .createdBy(roleAssignmentDBO.getCreatedBy())
        .build();
  }

  private String getResourceGroupIdentifier(ScopeLevel scopeLevel) {
    if (HarnessScopeLevel.PROJECT.equals(scopeLevel)) {
      return DEFAULT_PROJECT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
    } else if (HarnessScopeLevel.ORGANIZATION.equals(scopeLevel)) {
      return DEFAULT_ORGANIZATION_LEVEL_RESOURCE_GROUP_IDENTIFIER;
    } else {
      return DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
    }
  }
}