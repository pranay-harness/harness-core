/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
