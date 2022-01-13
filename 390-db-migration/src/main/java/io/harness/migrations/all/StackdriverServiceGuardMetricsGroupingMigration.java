/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StackdriverServiceGuardMetricsGroupingMigration implements Migration {
  @Inject CVConfigurationService cvConfigurationService;
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      try (HIterator<CVConfiguration> configRecords =
               new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class)
                                   .filter(CVConfigurationKeys.stateType, StateType.STACK_DRIVER)
                                   .filter(CVConfigurationKeys.enabled24x7, true)
                                   .fetch())) {
        while (configRecords.hasNext()) {
          CVConfiguration cvConfig = configRecords.next();
          wingsPersistence.delete(CVConfiguration.class, cvConfig.getUuid());
          log.info("Deleted CVConfig with uuid: " + cvConfig.getUuid());
          // recreate it
          cvConfig.setUuid(generateUuid());
          cvConfigurationService.saveConfiguration(
              cvConfig.getAccountId(), cvConfig.getAppId(), StateType.STACK_DRIVER, cvConfig);
          log.info("Recreated stackdriver CVConfig with uuid: " + cvConfig.getUuid());
        }
      }
    } catch (Exception ex) {
      log.error("Exception while executing StackdriverServiceGuardMetricsGroupingMigration", ex);
    }
  }
}
