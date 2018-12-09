package software.wings.service.impl.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.util.Collections;

/**
 * Created by Pranjal on 11/16/2018
 */
public class CVConfigurationServiceIntegrationTest extends BaseIntegrationTest {
  private NewRelicCVServiceConfiguration newRelicCVServiceConfiguration;

  private String createTestCVConfiguration() {
    createNewRelicConfig();
    return wingsPersistence.save(newRelicCVServiceConfiguration);
  }

  private void createNewRelicConfig() {
    String newRelicApplicationId = generateUuid();
    newRelicCVServiceConfiguration = new NewRelicCVServiceConfiguration();
    newRelicCVServiceConfiguration.setName("Config 1");
    newRelicCVServiceConfiguration.setAppId(generateUuid());
    newRelicCVServiceConfiguration.setEnvId("invalid_id_test_delete_config");
    newRelicCVServiceConfiguration.setServiceId(generateUuid());
    newRelicCVServiceConfiguration.setEnabled24x7(false);
    newRelicCVServiceConfiguration.setApplicationId(newRelicApplicationId);
    newRelicCVServiceConfiguration.setConnectorId(generateUuid());
    newRelicCVServiceConfiguration.setMetrics(Collections.singletonList("apdexScore"));
    newRelicCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
  }
}
