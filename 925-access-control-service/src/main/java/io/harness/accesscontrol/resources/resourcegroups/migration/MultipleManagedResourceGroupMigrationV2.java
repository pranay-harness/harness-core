package io.harness.accesscontrol.resources.resourcegroups.migration;

import static io.harness.NGConstants.DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.NGConstants.DEFAULT_ORGANIZATION_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.NGConstants.DEFAULT_PROJECT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.NGConstants.DEFAULT_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupRepository;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@Singleton
@OwnedBy(PL)
public class MultipleManagedResourceGroupMigrationV2 implements NGMigration {
  private final ResourceGroupRepository resourceGroupRepository;
  private static final List<String> managedResourceGroupsIdentifiers =
      Lists.newArrayList(DEFAULT_RESOURCE_GROUP_IDENTIFIER, DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER,
          DEFAULT_ORGANIZATION_LEVEL_RESOURCE_GROUP_IDENTIFIER, DEFAULT_PROJECT_LEVEL_RESOURCE_GROUP_IDENTIFIER);

  @Inject
  public MultipleManagedResourceGroupMigrationV2(ResourceGroupRepository resourceGroupRepository) {
    this.resourceGroupRepository = resourceGroupRepository;
  }

  @Override
  public void migrate() {
    log.info("Starting MultipleManagedResourceGroupMigrationV2....");
    Criteria criteria = Criteria.where(ResourceGroupDBO.ResourceGroupDBOKeys.scopeIdentifier)
                            .exists(true)
                            .ne(null)
                            .and(ResourceGroupDBO.ResourceGroupDBOKeys.identifier)
                            .in(managedResourceGroupsIdentifiers);
    if (resourceGroupRepository.deleteMulti(criteria)) {
      log.info("Successfully completed MultipleManagedResourceGroupMigrationV2.");
    } else {
      log.error("MultipleManagedResourceGroupMigrationV2 was not acknowledged.");
    }
  }
}
