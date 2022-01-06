package io.harness.accesscontrol.roleassignments.migration;

import static io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupConstants.DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupConstants.DEFAULT_ORGANIZATION_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupConstants.DEFAULT_PROJECT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupConstants.DEFAULT_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;

import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupService;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.harness.HarnessScopeLevel;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.utils.CryptoUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@Singleton
@OwnedBy(PL)
public class SuperAdminViaAllResourcesMigration implements NGMigration {
  private final RoleAssignmentRepository roleAssignmentRepository;
  private final HarnessResourceGroupService harnessResourceGroupService;
  private final ResourceGroupService resourceGroupService;

  @Inject
  public SuperAdminViaAllResourcesMigration(RoleAssignmentRepository roleAssignmentRepository,
      HarnessResourceGroupService harnessResourceGroupService, ResourceGroupService resourceGroupService) {
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.harnessResourceGroupService = harnessResourceGroupService;
    this.resourceGroupService = resourceGroupService;
  }

  @Override
  public void migrate() {
    while (!isAllResourcesResourceGroupUpdated()) {
      harnessResourceGroupService.sync(DEFAULT_RESOURCE_GROUP_IDENTIFIER, null);
      harnessResourceGroupService.sync(DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER, null);
      harnessResourceGroupService.sync(DEFAULT_ORGANIZATION_LEVEL_RESOURCE_GROUP_IDENTIFIER, null);
      harnessResourceGroupService.sync(DEFAULT_PROJECT_LEVEL_RESOURCE_GROUP_IDENTIFIER, null);
      sleep(Duration.ofSeconds(30));
    }
    migrateInternal(HarnessScopeLevel.ACCOUNT);
    migrateInternal(HarnessScopeLevel.ORGANIZATION);
  }

  private boolean isAllResourcesResourceGroupUpdated() {
    Optional<ResourceGroup> resourceGroupOptional =
        resourceGroupService.get(DEFAULT_RESOURCE_GROUP_IDENTIFIER, null, ManagedFilter.ONLY_MANAGED);
    if (resourceGroupOptional.isPresent()) {
      ResourceGroup resourceGroup = resourceGroupOptional.get();
      return resourceGroup.getResourceSelectors() != null && resourceGroup.getResourceSelectors().contains("/**/*/*");
    }
    return false;
  }

  private void migrateInternal(ScopeLevel scopeLevel) {
    int pageSize = 1000;
    int pageIndex = 0;
    Pageable pageable = PageRequest.of(pageIndex, pageSize);
    Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.scopeLevel)
                            .is(scopeLevel.toString())
                            .and(RoleAssignmentDBOKeys.roleIdentifier)
                            .in(getRoleIdentifiers(scopeLevel))
                            .and(RoleAssignmentDBOKeys.resourceGroupIdentifier)
                            .is(getResourceGroupIdentifier(scopeLevel));
    do {
      List<RoleAssignmentDBO> roleAssignmentList = roleAssignmentRepository.findAll(criteria, pageable).getContent();
      if (isEmpty(roleAssignmentList)) {
        return;
      }
      for (RoleAssignmentDBO roleAssignment : roleAssignmentList) {
        try {
          roleAssignmentRepository.save(buildRoleAssignmentDBO(roleAssignment));
        } catch (DuplicateKeyException exception) {
          log.info("[SuperAdminViaAllResourcesMigration] RoleAssignment already exists.", exception);
        }
        if (!"_account_viewer".equals(roleAssignment.getRoleIdentifier())) {
          roleAssignmentRepository.deleteById(roleAssignment.getId());
        }
      }
    } while (true);
  }

  private RoleAssignmentDBO buildRoleAssignmentDBO(RoleAssignmentDBO roleAssignmentDBO) {
    return RoleAssignmentDBO.builder()
        .identifier("role_assignment_".concat(CryptoUtils.secureRandAlphaNumString(20)))
        .scopeIdentifier(roleAssignmentDBO.getScopeIdentifier())
        .scopeLevel(roleAssignmentDBO.getScopeLevel())
        .disabled(roleAssignmentDBO.isDisabled())
        .managed(roleAssignmentDBO.isManaged())
        .roleIdentifier(roleAssignmentDBO.getRoleIdentifier())
        .resourceGroupIdentifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
        .principalIdentifier(roleAssignmentDBO.getPrincipalIdentifier())
        .principalType(roleAssignmentDBO.getPrincipalType())
        .createdAt(roleAssignmentDBO.getCreatedAt())
        .createdBy(roleAssignmentDBO.getCreatedBy())
        .build();
  }

  private String getResourceGroupIdentifier(ScopeLevel scopeLevel) {
    if (HarnessScopeLevel.ORGANIZATION.equals(scopeLevel)) {
      return DEFAULT_ORGANIZATION_LEVEL_RESOURCE_GROUP_IDENTIFIER;
    } else {
      return DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
    }
  }

  private List<String> getRoleIdentifiers(ScopeLevel scopeLevel) {
    if (HarnessScopeLevel.ORGANIZATION.equals(scopeLevel)) {
      return Lists.newArrayList("_organization_admin");
    } else {
      return Lists.newArrayList("_account_admin", "_account_viewer");
    }
  }
}
