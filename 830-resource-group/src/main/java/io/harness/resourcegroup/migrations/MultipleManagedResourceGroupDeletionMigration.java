package io.harness.resourcegroup.migrations;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.resourcegroup.framework.repositories.spring.ResourceGroupRepository;
import io.harness.resourcegroup.model.ResourceGroup.ResourceGroupKeys;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class MultipleManagedResourceGroupDeletionMigration implements NGMigration {
  private final ResourceGroupRepository resourceGroupRepository;

  @Inject
  public MultipleManagedResourceGroupDeletionMigration(ResourceGroupRepository resourceGroupRepository) {
    this.resourceGroupRepository = resourceGroupRepository;
  }

  @Override
  public void migrate() {
    Criteria criteria = Criteria.where(ResourceGroupKeys.accountIdentifier)
                            .exists(true)
                            .ne(null)
                            .and(ResourceGroupKeys.harnessManaged)
                            .is(Boolean.TRUE);
    try {
      if (!resourceGroupRepository.delete(criteria)) {
        log.error("Deletion of managed resource groups was not acknowledged.");
      }
    } catch (Exception exception) {
      log.error("Unexpected error occurred during ManagedResourceGroupMigrationJob.", exception);
    }
  }
}
