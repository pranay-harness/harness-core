/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.stackdriver.StackDriverMetricCVConfiguration;
import software.wings.verification.stackdriver.StackDriverMetricDefinition;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StackdriverMetricServiceGuardJsonMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Migrate stack driver metric service guard configurations");
    try (HIterator<CVConfiguration> cvConfigurationHIterator =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class)
                                 .filter("stateType", StateType.STACK_DRIVER.name())
                                 .fetch())) {
      while (cvConfigurationHIterator.hasNext()) {
        StackDriverMetricCVConfiguration cvConfiguration =
            (StackDriverMetricCVConfiguration) cvConfigurationHIterator.next();
        try {
          log.info("Processing config id {}", cvConfiguration.getUuid());
          cvConfiguration.getMetricDefinitions().forEach(metricDefinition -> {
            if (metricDefinition.checkIfOldFilter()) {
              String updatedFilterJson =
                  StackDriverMetricDefinition.getUpdatedFilterJson(metricDefinition.getFilterJson());
              metricDefinition.setFilterJson(updatedFilterJson);
              metricDefinition.extractJson();
            }
          });
          wingsPersistence.save(cvConfiguration);
          log.info("Saved config id {}", cvConfiguration.getUuid());
        } catch (Exception e) {
          log.error("Exception while updating config id: {}", cvConfiguration.getUuid(), e);
        }
      }
    }
    log.info("Migration completed for stack driver metric service guard configurations");
  }
}
