/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DatadogCustomMetricMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  private static final String GENERIC_GROUP_NAME = "TransactionGroup-1";

  @Override
  public void migrate() {
    List<CVConfiguration> datadogCVServiceConfigurations =
        wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority)
            .filter(CVConfigurationKeys.stateType, StateType.DATA_DOG)
            .asList();
    log.info("Found {} datadogCVConfigurations to potentially migrate.", datadogCVServiceConfigurations.size());
    try {
      List<DatadogCVServiceConfiguration> configsToSave = new ArrayList<>();
      if (isNotEmpty(datadogCVServiceConfigurations)) {
        for (CVConfiguration cvConfiguration : datadogCVServiceConfigurations) {
          DatadogCVServiceConfiguration datadogCVServiceConfiguration = (DatadogCVServiceConfiguration) cvConfiguration;
          if (isNotEmpty(datadogCVServiceConfiguration.getCustomMetrics())) {
            datadogCVServiceConfiguration.getCustomMetrics().forEach(
                (name, metricSet) -> { metricSet.forEach(metric -> metric.setTxnName(GENERIC_GROUP_NAME)); });
          }
          configsToSave.add(datadogCVServiceConfiguration);
        }
        log.info("Total number of Datadog configs with Custom Metrics {}", configsToSave.size());
        wingsPersistence.save(configsToSave);
      }
    } catch (Exception ex) {
      log.error("DatadogCustomMetricMigration failed", ex);
    }
  }
}
