/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;

import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.CloudWatchServiceImpl;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates.TimeSeriesMetricTemplatesKeys;
import software.wings.sm.StateType;
import software.wings.sm.states.CloudWatchState;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MigrateCloudwatchCVTemplates implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    List<CVConfiguration> cloudWatchConfigs = wingsPersistence.createQuery(CVConfiguration.class)
                                                  .filter(CVConfigurationKeys.stateType, StateType.CLOUD_WATCH)
                                                  .asList();

    if (isNotEmpty(cloudWatchConfigs)) {
      log.info("Migrating the templates of {} cloudwatch configs", cloudWatchConfigs.size());
      cloudWatchConfigs.forEach(config -> {
        // delete the existing template
        wingsPersistence.delete(wingsPersistence.createQuery(TimeSeriesMetricTemplates.class)
                                    .filter(TimeSeriesMetricTemplatesKeys.cvConfigId, config.getUuid()));

        // create the new template.
        TimeSeriesMetricTemplates metricTemplate;
        Map<String, TimeSeriesMetricDefinition> metricTemplates;
        metricTemplates = CloudWatchState.fetchMetricTemplates(
            CloudWatchServiceImpl.fetchMetrics((CloudWatchCVServiceConfiguration) config));
        metricTemplate = TimeSeriesMetricTemplates.builder()
                             .stateType(config.getStateType())
                             .metricTemplates(metricTemplates)
                             .cvConfigId(config.getUuid())
                             .build();
        metricTemplate.setAppId(config.getAppId());
        metricTemplate.setAccountId(config.getAccountId());
        metricTemplate.setStateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getUuid());
        wingsPersistence.save(metricTemplate);
        log.info("Migrated the metric template for {}", config.getUuid());
      });
    }
  }
}
