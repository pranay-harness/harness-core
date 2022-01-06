/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;

import com.google.inject.Inject;
import com.mongodb.DuplicateKeyException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppDTemplateMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    List<CVConfiguration> cvConfigurationList = wingsPersistence.createQuery(CVConfiguration.class)
                                                    .filter(CVConfigurationKeys.stateType, "APP_DYNAMICS")
                                                    .asList();

    log.info("Adding metric templates for {} APP_DYNAMICS cvConfigurations", cvConfigurationList.size());

    cvConfigurationList.forEach(cvConfiguration -> {
      try {
        TimeSeriesMetricTemplates metricTemplate =
            TimeSeriesMetricTemplates.builder()
                .stateType(cvConfiguration.getStateType())
                .metricTemplates(NewRelicMetricValueDefinition.APP_DYNAMICS_24X7_VALUES_TO_ANALYZE)
                .cvConfigId(cvConfiguration.getUuid())
                .build();
        metricTemplate.setAppId(cvConfiguration.getAppId());
        metricTemplate.setAccountId(cvConfiguration.getAccountId());
        wingsPersistence.save(metricTemplate);
      } catch (DuplicateKeyException ex) {
        log.info("Swallowing the DuplicateKeyException for cvConfig: {}", cvConfiguration.getUuid());
      }
    });
  }
}
