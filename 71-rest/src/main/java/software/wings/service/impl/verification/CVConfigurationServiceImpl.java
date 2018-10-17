package software.wings.service.impl.verification;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.sm.states.DatadogState;
import software.wings.sm.states.PrometheusState;
import software.wings.utils.JsonUtils;
import software.wings.verification.CVConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Vaibhav Tulsyan
 * 09/Oct/2018
 */

public class CVConfigurationServiceImpl implements CVConfigurationService {
  private static final Logger logger = LoggerFactory.getLogger(CVConfigurationServiceImpl.class);

  @Inject WingsPersistence wingsPersistence;

  public String saveConfiguration(String accountId, String appId, StateType stateType, Object params) {
    CVConfiguration cvConfiguration;
    switch (stateType) {
      case NEW_RELIC:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), NewRelicCVServiceConfiguration.class);
        break;

      case APP_DYNAMICS:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), AppDynamicsCVServiceConfiguration.class);
        break;

      case PROMETHEUS:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), PrometheusCVServiceConfiguration.class);
        break;

      case DATA_DOG:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), DatadogCVServiceConfiguration.class);
        break;

      default:
        throw new WingsException("No matching state type found " + stateType);
    }

    cvConfiguration.setAccountId(accountId);
    cvConfiguration.setAppId(appId);
    cvConfiguration.setStateType(stateType);
    wingsPersistence.save(cvConfiguration);
    saveMetricTemplate(appId, accountId, cvConfiguration, stateType);
    return cvConfiguration.getUuid();
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

    CVConfiguration updatedConfig;
    switch (stateType) {
      case NEW_RELIC:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), NewRelicCVServiceConfiguration.class);
        break;
      case APP_DYNAMICS:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), AppDynamicsCVServiceConfiguration.class);
        break;
      case PROMETHEUS:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), PrometheusCVServiceConfiguration.class);
        break;
      case DATA_DOG:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), DatadogCVServiceConfiguration.class);
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
    updateMetricTemplate(appId, accountId, updatedConfig, stateType);
    return savedConfiguration.getUuid();
  }

  public boolean deleteConfiguration(String accountId, String appId, String serviceConfigurationId) {
    Object savedConfig;
    savedConfig = wingsPersistence.get(CVConfiguration.class, serviceConfigurationId);
    if (savedConfig == null) {
      return false;
    }
    wingsPersistence.delete(CVConfiguration.class, serviceConfigurationId);
    deleteTemplate(accountId, serviceConfigurationId, ((CVConfiguration) savedConfig).getStateType());
    return true;
  }

  private void deleteTemplate(String accountId, String serviceConfigurationId, StateType stateType) {
    if (!stateType.equals(StateType.APP_DYNAMICS) && !stateType.equals(StateType.NEW_RELIC)) {
      TimeSeriesMetricTemplates timeSeriesMetricTemplates =
          wingsPersistence.createQuery(TimeSeriesMetricTemplates.class)
              .filter("cvConfigId", serviceConfigurationId)
              .filter("accountId", accountId)
              .get();
      wingsPersistence.delete(TimeSeriesMetricTemplates.class, timeSeriesMetricTemplates.getUuid());
    }
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

      case PROMETHEUS:
        updateOperations.set(
            "timeSeriesToAnalyze", ((PrometheusCVServiceConfiguration) cvConfiguration).getTimeSeriesToAnalyze());
        break;

      case DATA_DOG:
        updateOperations
            .set("datadogServiceName", ((DatadogCVServiceConfiguration) cvConfiguration).getDatadogServiceName())
            .set("metrics", ((DatadogCVServiceConfiguration) cvConfiguration).getMetrics());
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

  private void saveMetricTemplate(
      String appId, String accountId, CVConfiguration cvConfiguration, StateType stateType) {
    TimeSeriesMetricTemplates metricTemplate;
    Map<String, TimeSeriesMetricDefinition> metricTemplates;
    switch (stateType) {
      case APP_DYNAMICS:
      case NEW_RELIC:
        return;
      case PROMETHEUS:
        metricTemplates = PrometheusState.createMetricTemplates(
            ((PrometheusCVServiceConfiguration) cvConfiguration).getTimeSeriesToAnalyze());
        metricTemplate = TimeSeriesMetricTemplates.builder()
                             .stateType(stateType)
                             .metricTemplates(metricTemplates)
                             .cvConfigId(cvConfiguration.getUuid())
                             .build();
        break;
      case DATA_DOG:
        List<String> metricNames =
            Arrays.asList(((DatadogCVServiceConfiguration) cvConfiguration).getMetrics().split(","));
        metricTemplates = DatadogState.metricDefinitions(DatadogState.metrics(metricNames).values());
        metricTemplate = TimeSeriesMetricTemplates.builder()
                             .stateType(stateType)
                             .metricTemplates(metricTemplates)
                             .cvConfigId(cvConfiguration.getUuid())
                             .build();
        break;
      default:
        throw new WingsException("No matching state type found " + stateType);
    }
    metricTemplate.setAppId(appId);
    metricTemplate.setAccountId(accountId);
    wingsPersistence.save(metricTemplate);
  }

  private void updateMetricTemplate(
      String appId, String accountId, CVConfiguration cvConfiguration, StateType stateType) {
    TimeSeriesMetricTemplates metricTemplate;
    Map<String, TimeSeriesMetricDefinition> metricTemplates;
    switch (stateType) {
      case APP_DYNAMICS:
      case NEW_RELIC:
        // Uses default metric template no Metric Template needs to be persisted
        return;
      case PROMETHEUS:
        metricTemplates = PrometheusState.createMetricTemplates(
            ((PrometheusCVServiceConfiguration) cvConfiguration).getTimeSeriesToAnalyze());
        metricTemplate = TimeSeriesMetricTemplates.builder()
                             .stateType(stateType)
                             .metricTemplates(metricTemplates)
                             .cvConfigId(cvConfiguration.getUuid())
                             .build();
        break;
      case DATA_DOG:
        List<String> metricNames =
            Arrays.asList(((DatadogCVServiceConfiguration) cvConfiguration).getMetrics().split(","));
        metricTemplates = DatadogState.metricDefinitions(DatadogState.metrics(metricNames).values());
        metricTemplate = TimeSeriesMetricTemplates.builder()
                             .stateType(stateType)
                             .metricTemplates(metricTemplates)
                             .cvConfigId(cvConfiguration.getUuid())
                             .build();
        break;
      default:
        throw new WingsException("No matching state type found " + stateType);
    }
    metricTemplate.setAppId(appId);
    metricTemplate.setAccountId(accountId);

    UpdateOperations<TimeSeriesMetricTemplates> updateOperations =
        wingsPersistence.createUpdateOperations(TimeSeriesMetricTemplates.class)
            .set("stateType", cvConfiguration.getStateType())
            .set("cvConfigId", cvConfiguration.getUuid())
            .set("metricTemplates", metricTemplate)
            .set("appId", appId)
            .set("accountId", accountId);

    TimeSeriesMetricTemplates savedTimeSeriesMetricTemplates =
        wingsPersistence.createQuery(TimeSeriesMetricTemplates.class)
            .filter("cvConfigId", cvConfiguration.getUuid())
            .filter("appId", appId)
            .filter("accountId", accountId)
            .filter("stateType", cvConfiguration.getStateType())
            .get();

    wingsPersistence.update(savedTimeSeriesMetricTemplates, updateOperations);
  }
}
