package software.wings.service.impl.verification;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.service.intfc.verification.NewRelicCVConfigurationService;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;
import software.wings.verification.CVConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.util.List;

/**
 * @author Vaibhav Tulsyan
 * 09/Oct/2018
 */

public class CVConfigurationServiceImpl implements CVConfigurationService {
  private static final Logger logger = LoggerFactory.getLogger(CVConfigurationServiceImpl.class);

  @Inject WingsPersistence wingsPersistence;
  @Inject NewRelicCVConfigurationService newRelicCVConfigurationService;

  public String saveConfiguration(String accountId, String appId, StateType stateType, Object params) {
    CVConfiguration cvConfiguration = null;
    switch (stateType) {
      case NEW_RELIC:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), NewRelicCVServiceConfiguration.class);
        break;

      case APP_DYNAMICS:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), AppDynamicsCVServiceConfiguration.class);
        break;

      default:
        throw new WingsException("No matching state type found " + stateType);
    }

    cvConfiguration.setAccountId(accountId);
    cvConfiguration.setAppId(appId);
    cvConfiguration.setStateType(stateType);
    return wingsPersistence.save(cvConfiguration);
  }

  public <T extends CVConfiguration> T getConfiguration(String serviceConfigurationId) {
    return (T) wingsPersistence.get(CVConfiguration.class, serviceConfigurationId);
  }

  @Override
  public <T extends CVConfiguration> List<T> listConfigurations(String accountId, String appId) {
    return (List<T>) wingsPersistence.createQuery(CVConfiguration.class)
        .filter("accountId", accountId)
        .filter("appId", appId)
        .asList();
  }

  public String updateConfiguration(
      String accountId, String appId, StateType stateType, Object params, String serviceConfigurationId) {
    logger.info("Updating CV service configuration id " + serviceConfigurationId);
    switch (stateType) {
      case NEW_RELIC:
        NewRelicCVServiceConfiguration savedConfiguration =
            (NewRelicCVServiceConfiguration) wingsPersistence.get(CVConfiguration.class, appId, serviceConfigurationId);
        NewRelicCVServiceConfiguration obj =
            JsonUtils.asObject(JsonUtils.asJson(params), NewRelicCVServiceConfiguration.class);
        UpdateOperations<CVConfiguration> updateOperations = newRelicCVConfigurationService.getUpdateOperations(obj);
        wingsPersistence.update(savedConfiguration, updateOperations);
        return savedConfiguration.getUuid();
      default:
        throw new WingsException("No matching state type found - " + stateType)
            .addParam("accountId", accountId)
            .addParam("appId", appId)
            .addParam("serviceConfigurationId", serviceConfigurationId)
            .addParam("stateType", String.valueOf(stateType));
    }
  }

  public boolean deleteConfiguration(String accountId, String appId, String serviceConfigurationId) {
    Object savedConfig;
    savedConfig = wingsPersistence.get(CVConfiguration.class, serviceConfigurationId);
    if (savedConfig == null) {
      return false;
    }
    wingsPersistence.delete(CVConfiguration.class, serviceConfigurationId);
    return true;
  }

  @Override
  public <T extends CVConfiguration> List<T> listConfigurations(String accountId) {
    return (List<T>) wingsPersistence.createQuery(CVConfiguration.class).filter("accountId", accountId).asList();
  }
}
