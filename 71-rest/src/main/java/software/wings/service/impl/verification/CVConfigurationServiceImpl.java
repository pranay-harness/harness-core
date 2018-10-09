package software.wings.service.impl.verification;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;
import software.wings.verification.CVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

/**
 * @author Vaibhav Tulsyan
 * 09/Oct/2018
 */

public class CVConfigurationServiceImpl implements CVConfigurationService {
  @Inject WingsPersistence wingsPersistence;

  public String saveConfiguration(StateType stateType, Object params) {
    switch (stateType) {
      case NEW_RELIC:
        NewRelicCVServiceConfiguration obj =
            JsonUtils.asObject(JsonUtils.asJson(params), NewRelicCVServiceConfiguration.class);
        return wingsPersistence.save(obj);
      default:
        throw new WingsException("No matching state type found");
    }
  }

  public <T extends CVConfiguration> T getConfiguration(String serviceConfigurationId) {
    return (T) wingsPersistence.get(CVConfiguration.class, serviceConfigurationId);
  }
}
