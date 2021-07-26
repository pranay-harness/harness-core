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
