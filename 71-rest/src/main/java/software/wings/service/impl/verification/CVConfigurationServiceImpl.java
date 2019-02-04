package software.wings.service.impl.verification;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.CloudWatchServiceImpl;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.sm.states.CloudWatchState;
import software.wings.sm.states.DatadogState;
import software.wings.sm.states.PrometheusState;
import software.wings.verification.CVConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Vaibhav Tulsyan
 * 09/Oct/2018
 */
@Singleton
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

      case DYNA_TRACE:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), DynaTraceCVServiceConfiguration.class);
        break;

      case PROMETHEUS:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), PrometheusCVServiceConfiguration.class);
        break;

      case DATA_DOG:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), DatadogCVServiceConfiguration.class);
        break;

      case CLOUD_WATCH:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), CloudWatchCVServiceConfiguration.class);
        break;

      case SUMO:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), LogsCVConfiguration.class);
        break;

      case ELK:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), ElkCVConfiguration.class);
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

  public <T extends CVConfiguration> T getConfiguration(String name, String appId, String envId) {
    CVConfiguration cvConfiguration = wingsPersistence.createQuery(CVConfiguration.class)
                                          .filter("name", name)
                                          .filter("appId", appId)
                                          .filter("envId", envId)
                                          .get();

    fillInServiceAndConnectorNames(cvConfiguration);
    return (T) cvConfiguration;
  }

  public String updateConfiguration(CVConfiguration cvConfiguration, String appId) {
    CVConfiguration savedConfiguration =
        wingsPersistence.getWithAppId(CVConfiguration.class, appId, cvConfiguration.getUuid());
    UpdateOperations<CVConfiguration> updateOperations =
        getUpdateOperations(cvConfiguration.getStateType(), cvConfiguration);
    wingsPersistence.update(savedConfiguration, updateOperations);
    return savedConfiguration.getUuid();
  }

  public String saveCofiguration(CVConfiguration cvConfiguration) {
    CVConfiguration config = wingsPersistence.saveAndGet(CVConfiguration.class, cvConfiguration);
    return config.getUuid();
  }
  @Override
  public <T extends CVConfiguration> List<T> listConfigurations(
      String accountId, String appId, String envId, StateType stateType) {
    Query<T> configurationQuery = (Query<T>) wingsPersistence.createQuery(CVConfiguration.class)
                                      .filter(ACCOUNT_ID, accountId)
                                      .filter("appId", appId);
    if (isNotEmpty(envId)) {
      configurationQuery = configurationQuery.filter("envId", envId);
    }

    if (stateType != null) {
      configurationQuery = configurationQuery.filter("stateType", stateType);
    }

    List<T> cvConfigurations = configurationQuery.asList();
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
      case DYNA_TRACE:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), DynaTraceCVServiceConfiguration.class);
        break;
      case PROMETHEUS:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), PrometheusCVServiceConfiguration.class);
        break;
      case DATA_DOG:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), DatadogCVServiceConfiguration.class);
        break;
      case CLOUD_WATCH:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), CloudWatchCVServiceConfiguration.class);
        break;
      case SUMO:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), LogsCVConfiguration.class);
        break;
      case ELK:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), ElkCVConfiguration.class);
        break;
      default:
        throw new WingsException("No matching state type found - " + stateType)
            .addParam(ACCOUNT_ID, accountId)
            .addParam("appId", appId)
            .addParam("serviceConfigurationId", serviceConfigurationId)
            .addParam("stateType", String.valueOf(stateType));
    }
    CVConfiguration savedConfiguration =
        wingsPersistence.getWithAppId(CVConfiguration.class, appId, serviceConfigurationId);
    UpdateOperations<CVConfiguration> updateOperations = getUpdateOperations(stateType, updatedConfig);
    wingsPersistence.update(savedConfiguration, updateOperations);
    // TODO update metric template if it makes sense
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
              .filter(ACCOUNT_ID, accountId)
              .get();
      if (timeSeriesMetricTemplates != null) {
        wingsPersistence.delete(TimeSeriesMetricTemplates.class, timeSeriesMetricTemplates.getUuid());
      }
    }
  }

  @Override
  public <T extends CVConfiguration> List<T> listConfigurations(String accountId) {
    return (List<T>) wingsPersistence.createQuery(CVConfiguration.class).filter(ACCOUNT_ID, accountId).asList();
  }

  @Override
  public List<CVConfiguration> listConfigurations(String accountId, PageRequest<CVConfiguration> pageRequest) {
    pageRequest.addFilter(ACCOUNT_ID, Operator.EQ, accountId);
    return wingsPersistence.query(CVConfiguration.class, pageRequest).getResponse();
  }

  private UpdateOperations<CVConfiguration> getUpdateOperations(StateType stateType, CVConfiguration cvConfiguration) {
    logger.info("Updating CV Service Configuration {}", cvConfiguration);
    UpdateOperations<CVConfiguration> updateOperations =
        wingsPersistence.createUpdateOperations(CVConfiguration.class)
            .set("connectorId", cvConfiguration.getConnectorId())
            .set("envId", cvConfiguration.getEnvId())
            .set("serviceId", cvConfiguration.getServiceId())
            .set("enabled24x7", cvConfiguration.isEnabled24x7())
            .set("analysisTolerance", cvConfiguration.getAnalysisTolerance())
            .set("name", cvConfiguration.getName());
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
      case DYNA_TRACE:
        updateOperations.set("serviceMethods", ((DynaTraceCVServiceConfiguration) cvConfiguration).getServiceMethods());
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
      case CLOUD_WATCH:
        updateOperations
            .set("loadBalancerMetrics", ((CloudWatchCVServiceConfiguration) cvConfiguration).getLoadBalancerMetrics())
            .set("region", ((CloudWatchCVServiceConfiguration) cvConfiguration).getRegion())
            .set("lambdaFunctions", ((CloudWatchCVServiceConfiguration) cvConfiguration).getLambdaFunctions())
            .set("clusterName", ((CloudWatchCVServiceConfiguration) cvConfiguration).getClusterName());
        break;
      case SUMO:
        updateOperations.set("query", ((LogsCVConfiguration) cvConfiguration).getQuery())
            .set("formattedQuery", ((LogsCVConfiguration) cvConfiguration).isFormattedQuery());
        break;
      case ELK:
        updateOperations.set("query", ((ElkCVConfiguration) cvConfiguration).getQuery())
            .set("formattedQuery", ((ElkCVConfiguration) cvConfiguration).isFormattedQuery())
            .set("queryType", ((ElkCVConfiguration) cvConfiguration).getQueryType())
            .set("index", ((ElkCVConfiguration) cvConfiguration).getIndex())
            .set("messageField", ((ElkCVConfiguration) cvConfiguration).getMessageField())
            .set("timestampField", ((ElkCVConfiguration) cvConfiguration).getTimestampField())
            .set("timestampFormat", ((ElkCVConfiguration) cvConfiguration).getTimestampFormat());
        break;
      default:
        throw new IllegalStateException("Invalid state type: " + stateType);
    }

    return updateOperations;
  }

  public void fillInServiceAndConnectorNames(CVConfiguration cvConfiguration) {
    Service service = wingsPersistence.get(Service.class, cvConfiguration.getServiceId());
    if (service != null) {
      cvConfiguration.setServiceName(service.getName());
    }

    SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, cvConfiguration.getConnectorId());
    if (settingAttribute != null) {
      cvConfiguration.setConnectorName(settingAttribute.getName());
    }

    Environment environment = wingsPersistence.get(Environment.class, cvConfiguration.getEnvId());
    if (environment != null) {
      cvConfiguration.setEnvName(environment.getName());
    }
    Application app = wingsPersistence.get(Application.class, cvConfiguration.getAppId());
    if (app != null) {
      cvConfiguration.setAppName(app.getName());
    }
  }

  private void saveMetricTemplate(
      String appId, String accountId, CVConfiguration cvConfiguration, StateType stateType) {
    TimeSeriesMetricTemplates metricTemplate;
    Map<String, TimeSeriesMetricDefinition> metricTemplates;
    switch (stateType) {
      case APP_DYNAMICS:
        metricTemplate = TimeSeriesMetricTemplates.builder()
                             .stateType(stateType)
                             .metricTemplates(NewRelicMetricValueDefinition.APP_DYNAMICS_24X7_VALUES_TO_ANALYZE)
                             .cvConfigId(cvConfiguration.getUuid())
                             .build();
        break;
      case NEW_RELIC:
      case DYNA_TRACE:
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
      case CLOUD_WATCH:
        metricTemplates = CloudWatchState.fetchMetricTemplates(CloudWatchServiceImpl.fetchMetrics());
        metricTemplate = TimeSeriesMetricTemplates.builder()
                             .stateType(stateType)
                             .metricTemplates(metricTemplates)
                             .cvConfigId(cvConfiguration.getUuid())
                             .build();
        break;

      case SUMO:
      case ELK:
        return;

      default:
        throw new WingsException("No matching state type found " + stateType);
    }
    metricTemplate.setAppId(appId);
    metricTemplate.setAccountId(accountId);
    wingsPersistence.save(metricTemplate);
  }

  public void deleteStaleConfigs() {
    List<CVConfiguration> cvConfigurationList =
        wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority).asList();

    Set<String> deleteList = new HashSet<>();
    for (CVConfiguration configuration : cvConfigurationList) {
      Environment environment = wingsPersistence.get(Environment.class, configuration.getEnvId());
      if (environment == null) {
        deleteList.add(configuration.getUuid());
        continue;
      }
      Application app = wingsPersistence.get(Application.class, configuration.getAppId());
      if (app == null) {
        deleteList.add(configuration.getUuid());
      }
    }

    logger.info("Deleting {} stale CVConfigurations: {}", deleteList.size(), deleteList);

    Query<CVConfiguration> query = wingsPersistence.createQuery(CVConfiguration.class).field("_id").in(deleteList);

    wingsPersistence.delete(query);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(wingsPersistence.createQuery(CVConfiguration.class).filter(ACCOUNT_ID, accountId));
  }
}
