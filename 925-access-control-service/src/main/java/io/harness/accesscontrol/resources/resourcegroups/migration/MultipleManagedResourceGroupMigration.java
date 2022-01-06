/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.resources.resourcegroups.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO.ResourceGroupDBOKeys;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupRepository;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@Singleton
@OwnedBy(PL)
public class MultipleManagedResourceGroupMigration implements NGMigration {
  private final ResourceGroupRepository resourceGroupRepository;

  @Inject
  public MultipleManagedResourceGroupMigration(ResourceGroupRepository resourceGroupRepository) {
    this.resourceGroupRepository = resourceGroupRepository;
  }

  @Override
  public void migrate() {
    log.info("Starting MultipleManagedResourceGroupMigration....");
    Criteria criteria = Criteria.where(ResourceGroupDBOKeys.scopeIdentifier)
                            .exists(true)
                            .ne(null)
                            .and(ResourceGroupDBOKeys.identifier)
                            .is("_all_resources");
    if (resourceGroupRepository.deleteMulti(criteria)) {
      log.info("Successfully completed MultipleManagedResourceGroupMigration.");
    } else {
      log.error("MultipleManagedResourceGroupMigration was not acknowledged.");
    }
  }
}
