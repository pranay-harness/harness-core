package migrations.all;

import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.verification.CVConfiguration;

import java.util.List;

public class AppDTemplateMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AppDTemplateMigration.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    List<CVConfiguration> cvConfigurationList =
        wingsPersistence.createQuery(CVConfiguration.class).filter("stateType", "APP_DYNAMICS").asList();

    logger.info("Adding metric templates for {} APP_DYNAMICS cvConfigurations", cvConfigurationList.size());

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
        logger.info("Swallowing the DuplicateKeyException for cvConfig: {}", cvConfiguration.getUuid());
      }
    });
  }
}
