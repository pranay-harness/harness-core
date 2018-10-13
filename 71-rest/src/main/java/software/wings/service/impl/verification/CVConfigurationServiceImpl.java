package software.wings.service.impl.verification;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.verification.CVConfigurationService;
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
    CVConfiguration cvConfiguration = wingsPersistence.get(CVConfiguration.class, serviceConfigurationId);
    fillInServiceAndConnectorNames(cvConfiguration);
    return (T) cvConfiguration;
  }

  @Override
  public <T extends CVConfiguration> List<T> listConfigurations(String accountId, String appId) {
    List<T> cvConfigurations = (List<T>) wingsPersistence.createQuery(CVConfiguration.class)
                                   .filter("accountId", accountId)
                                   .filter("appId", appId)
                                   .asList();
    cvConfigurations.forEach(cvConfiguration -> fillInServiceAndConnectorNames(cvConfiguration));
    return cvConfigurations;
  }

  public String updateConfiguration(
      String accountId, String appId, StateType stateType, Object params, String serviceConfigurationId) {
    logger.info("Updating CV service configuration id " + serviceConfigurationId);

    CVConfiguration updatedConfig = null;
    switch (stateType) {
      case NEW_RELIC:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), NewRelicCVServiceConfiguration.class);
        break;
      case APP_DYNAMICS:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), AppDynamicsCVServiceConfiguration.class);
        break;
      default:
        throw new WingsException("No matching state type found - " + stateType)
            .addParam("accountId", accountId)
            .addParam("appId", appId)
            .addParam("serviceConfigurationId", serviceConfigurationId)
            .addParam("stateType", String.valueOf(stateType));
    }
    CVConfiguration savedConfiguration = wingsPersistence.get(CVConfiguration.class, appId, serviceConfigurationId);
    UpdateOperations<CVConfiguration> updateOperations = getUpdateOperations(stateType, updatedConfig);
    wingsPersistence.update(savedConfiguration, updateOperations);
    return savedConfiguration.getUuid();
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

  private UpdateOperations<CVConfiguration> getUpdateOperations(StateType stateType, CVConfiguration cvConfiguration) {
    logger.info("Updating CV Service Configuration {}", cvConfiguration);
    UpdateOperations<CVConfiguration> updateOperations =
        wingsPersistence.createUpdateOperations(CVConfiguration.class)
            .set("connectorId", cvConfiguration.getConnectorId())
            .set("envId", cvConfiguration.getEnvId())
            .set("serviceId", cvConfiguration.getServiceId())
            .set("enabled24x7", cvConfiguration.isEnabled24x7())
            .set("analysisTolerance", cvConfiguration.getAnalysisTolerance());
    switch (stateType) {
      case NEW_RELIC:
        updateOperations.set("applicationId", ((NewRelicCVServiceConfiguration) cvConfiguration).getApplicationId())
            .set("metrics", ((NewRelicCVServiceConfiguration) cvConfiguration).getMetrics());
        break;

      case APP_DYNAMICS:
        updateOperations
            .set("appDynamicsApplicationId",
                ((AppDynamicsCVServiceConfiguration) cvConfiguration).getAppDynamicsApplicationId())
            .set("tierId", ((AppDynamicsCVServiceConfiguration) cvConfiguration).getTierId());
        break;

      default:
        throw new IllegalStateException("Invalid state type: " + stateType);
    }

    return updateOperations;
  }

  private void fillInServiceAndConnectorNames(CVConfiguration cvConfiguration) {
    Service service = wingsPersistence.get(Service.class, cvConfiguration.getServiceId());
    if (service != null) {
      cvConfiguration.setServiceName(service.getName());
    }

    SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, cvConfiguration.getConnectorId());
    if (settingAttribute != null) {
      cvConfiguration.setConnectorName(settingAttribute.getName());
    }
  }
}
