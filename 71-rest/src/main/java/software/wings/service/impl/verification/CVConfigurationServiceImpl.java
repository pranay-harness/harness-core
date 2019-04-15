package software.wings.service.impl.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.CloudWatchServiceImpl;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
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
import software.wings.verification.log.BugsnagCVConfiguration;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Vaibhav Tulsyan
 * 09/Oct/2018
 */
@Singleton
@Slf4j
public class CVConfigurationServiceImpl implements CVConfigurationService {
  @Inject WingsPersistence wingsPersistence;
  @Inject CvValidationService cvValidationService;

  @Override
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
        ElkCVConfiguration elkCVConfiguration = (ElkCVConfiguration) cvConfiguration;
        cvValidationService.validateELKQuery(accountId, appId, elkCVConfiguration.getConnectorId(),
            elkCVConfiguration.getQuery(), elkCVConfiguration.getIndex());
        break;
      case BUG_SNAG:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), BugsnagCVConfiguration.class);
        break;

      default:
        throw new WingsException("No matching state type found " + stateType);
    }

    cvConfiguration.setAccountId(accountId);
    cvConfiguration.setAppId(appId);
    cvConfiguration.setStateType(stateType);
    cvConfiguration.setUuid(generateUuid());
    try {
      saveMetricTemplate(appId, accountId, cvConfiguration, stateType);
      wingsPersistence.save(cvConfiguration);
    } catch (DuplicateKeyException ex) {
      throw new WingsException("A Service Verification with the name " + cvConfiguration.getName()
          + " already exists. Please choose a unique name.");
    }

    return cvConfiguration.getUuid();
  }

  @Override
  public <T extends CVConfiguration> T getConfiguration(String serviceConfigurationId) {
    CVConfiguration cvConfiguration = wingsPersistence.get(CVConfiguration.class, serviceConfigurationId);
    if (cvConfiguration == null) {
      throw new IllegalArgumentException("No CV Configuration found for Id " + serviceConfigurationId);
    }
    fillInServiceAndConnectorNames(cvConfiguration);
    return (T) cvConfiguration;
  }

  @Override
  public <T extends CVConfiguration> T getConfiguration(String name, String appId, String envId) {
    CVConfiguration cvConfiguration = wingsPersistence.createQuery(CVConfiguration.class)
                                          .filter("name", name)
                                          .filter("appId", appId)
                                          .filter("envId", envId)
                                          .get();

    if (cvConfiguration == null) {
      return null;
    }
    fillInServiceAndConnectorNames(cvConfiguration);
    return (T) cvConfiguration;
  }

  public String updateConfiguration(CVConfiguration cvConfiguration, String appId) {
    CVConfiguration savedConfiguration =
        wingsPersistence.getWithAppId(CVConfiguration.class, appId, cvConfiguration.getUuid());

    UpdateOperations<CVConfiguration> updateOperations =
        getUpdateOperations(cvConfiguration.getStateType(), cvConfiguration, savedConfiguration);
    try {
      wingsPersistence.update(savedConfiguration, updateOperations);
    } catch (DuplicateKeyException ex) {
      throw new WingsException("A Service Verification with the name " + cvConfiguration.getName()
          + " already exists. Please choose a unique name.");
    }
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
                                      .filter(CVConfiguration.ACCOUNT_ID_KEY, accountId)
                                      .filter("appId", appId);
    if (isNotEmpty(envId)) {
      configurationQuery = configurationQuery.filter("envId", envId);
    }

    if (stateType != null) {
      configurationQuery = configurationQuery.filter("stateType", stateType);
    }

    List<T> cvConfigurations = configurationQuery.asList();

    List<T> cvConfigurations24x7 = new ArrayList<>();
    // filter out cv configurations that were created for workflow
    cvConfigurations.forEach(cvConfiguration -> {
      if (!cvConfiguration.isWorkflowConfig()) {
        fillInServiceAndConnectorNames(cvConfiguration);
        cvConfigurations24x7.add(cvConfiguration);
      }
    });
    return cvConfigurations24x7;
  }

  @Override
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
        ElkCVConfiguration elkCVConfiguration = (ElkCVConfiguration) updatedConfig;
        cvValidationService.validateELKQuery(accountId, appId, elkCVConfiguration.getConnectorId(),
            elkCVConfiguration.getQuery(), elkCVConfiguration.getIndex());
        break;
      case BUG_SNAG:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), BugsnagCVConfiguration.class);
        break;
      default:
        throw new WingsException("No matching state type found - " + stateType)
            .addParam(CVConfiguration.ACCOUNT_ID_KEY, accountId)
            .addParam("appId", appId)
            .addParam("serviceConfigurationId", serviceConfigurationId)
            .addParam("stateType", String.valueOf(stateType));
    }
    CVConfiguration savedConfiguration =
        wingsPersistence.getWithAppId(CVConfiguration.class, appId, serviceConfigurationId);
    UpdateOperations<CVConfiguration> updateOperations =
        getUpdateOperations(stateType, updatedConfig, savedConfiguration);
    try {
      wingsPersistence.update(savedConfiguration, updateOperations);
    } catch (DuplicateKeyException ex) {
      throw new WingsException("A Service Verification with the name " + updatedConfig.getName()
          + " already exists. Please choose a unique name.");
    }
    // TODO update metric template if it makes sense
    return savedConfiguration.getUuid();
  }

  @Override
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

  @Override
  public boolean resetBaseline(String appId, String cvConfigId, LogsCVConfiguration logsCVConfiguration) {
    final LogsCVConfiguration cvConfiguration = wingsPersistence.get(LogsCVConfiguration.class, cvConfigId);
    if (cvConfiguration == null) {
      throw new WingsException(GENERAL_ERROR, USER).addParam("message", "No configuration found with id " + cvConfigId);
    }

    if (logsCVConfiguration == null) {
      throw new WingsException(GENERAL_ERROR, USER).addParam("message", "No log configuration provided in the payload");
    }

    if (logsCVConfiguration.getBaselineStartMinute() <= 0 || logsCVConfiguration.getBaselineEndMinute() <= 0
        || logsCVConfiguration.getBaselineEndMinute()
            < logsCVConfiguration.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES - 1) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message",
              "Invalid baseline start and end time provided. They both should be positive and the difference should at least be "
                  + (CRON_POLL_INTERVAL_IN_MINUTES - 1) + " provided config: " + logsCVConfiguration);
    }
    wingsPersistence.delete(
        wingsPersistence.createQuery(LogDataRecord.class).filter("appId", appId).filter("cvConfigId", cvConfigId));
    wingsPersistence.delete(wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                .filter("appId", appId)
                                .filter("cvConfigId", cvConfigId)
                                .field("logCollectionMinute")
                                .greaterThanOrEq(logsCVConfiguration.getBaselineStartMinute()));
    wingsPersistence.update(wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                .filter("appId", appId)
                                .filter("cvConfigId", cvConfigId)
                                .field("logCollectionMinute")
                                .lessThanOrEq(logsCVConfiguration.getBaselineStartMinute()),
        wingsPersistence.createUpdateOperations(LogMLAnalysisRecord.class).set("deprecated", true));
    wingsPersistence.delete(wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                .filter("appId", appId)
                                .filter("cvConfigId", cvConfigId));
    cvConfiguration.setBaselineStartMinute(logsCVConfiguration.getBaselineStartMinute());
    cvConfiguration.setBaselineEndMinute(logsCVConfiguration.getBaselineEndMinute());
    wingsPersistence.save(cvConfiguration);
    return true;
  }

  private void deleteTemplate(String accountId, String serviceConfigurationId, StateType stateType) {
    if (!stateType.equals(StateType.APP_DYNAMICS) && !stateType.equals(StateType.NEW_RELIC)) {
      TimeSeriesMetricTemplates timeSeriesMetricTemplates =
          wingsPersistence.createQuery(TimeSeriesMetricTemplates.class)
              .filter("cvConfigId", serviceConfigurationId)
              .filter(TimeSeriesMetricTemplates.ACCOUNT_ID_KEY, accountId)
              .get();
      if (timeSeriesMetricTemplates != null) {
        wingsPersistence.delete(TimeSeriesMetricTemplates.class, timeSeriesMetricTemplates.getUuid());
      }
    }
  }

  @Override
  public <T extends CVConfiguration> List<T> listConfigurations(String accountId) {
    return (List<T>) wingsPersistence.createQuery(CVConfiguration.class)
        .filter(CVConfiguration.ACCOUNT_ID_KEY, accountId)
        .asList();
  }

  @Override
  public List<CVConfiguration> listConfigurations(String accountId, PageRequest<CVConfiguration> pageRequest) {
    pageRequest.addFilter(CVConfiguration.ACCOUNT_ID_KEY, Operator.EQ, accountId);
    return wingsPersistence.query(CVConfiguration.class, pageRequest).getResponse();
  }

  private UpdateOperations<CVConfiguration> getUpdateOperations(
      StateType stateType, CVConfiguration cvConfiguration, CVConfiguration savedConfiguration) {
    logger.info("Updating CV Service Configuration {}", cvConfiguration);
    UpdateOperations<CVConfiguration> updateOperations =
        wingsPersistence.createUpdateOperations(CVConfiguration.class)
            .set("connectorId", cvConfiguration.getConnectorId())
            .set("envId", cvConfiguration.getEnvId())
            .set("serviceId", cvConfiguration.getServiceId())
            .set("enabled24x7", cvConfiguration.isEnabled24x7())
            .set("analysisTolerance", cvConfiguration.getAnalysisTolerance())
            .set("name", cvConfiguration.getName())
            .set("alertEnabled", cvConfiguration.isAlertEnabled())
            .set("alertThreshold", cvConfiguration.getAlertThreshold())
            .set("snoozeStartTime", cvConfiguration.getSnoozeStartTime())
            .set("snoozeEndTime", cvConfiguration.getSnoozeEndTime());
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
            .set("metrics", ((DatadogCVServiceConfiguration) cvConfiguration).getMetrics())
            .set("applicationFilter", ((DatadogCVServiceConfiguration) cvConfiguration).getApplicationFilter());
        break;
      case CLOUD_WATCH:
        CloudWatchCVServiceConfiguration cloudWatchCVServiceConfiguration =
            (CloudWatchCVServiceConfiguration) cvConfiguration;
        if (isEmpty(cloudWatchCVServiceConfiguration.getLoadBalancerMetrics())
            && isEmpty(cloudWatchCVServiceConfiguration.getEc2InstanceNames())
            && isEmpty(cloudWatchCVServiceConfiguration.getLambdaFunctionsMetrics())
            && isEmpty(cloudWatchCVServiceConfiguration.getEcsMetrics())) {
          throw new WingsException("No metric provided in Configuration for configId " + savedConfiguration.getUuid()
              + " and serviceId " + savedConfiguration.getServiceId());
        }
        updateOperations.set("region", cloudWatchCVServiceConfiguration.getRegion());

        if (isNotEmpty(cloudWatchCVServiceConfiguration.getLoadBalancerMetrics())) {
          updateOperations.set("loadBalancerMetrics", cloudWatchCVServiceConfiguration.getLoadBalancerMetrics());
        } else if (isNotEmpty(((CloudWatchCVServiceConfiguration) savedConfiguration).getLoadBalancerMetrics())) {
          updateOperations.unset("loadBalancerMetrics");
        }
        if (isNotEmpty(cloudWatchCVServiceConfiguration.getEc2InstanceNames())) {
          updateOperations.set("ec2InstanceName", cloudWatchCVServiceConfiguration.getEc2InstanceNames())
              .set("ec2Metrics", cloudWatchCVServiceConfiguration.getEc2Metrics());
        } else if (isNotEmpty(((CloudWatchCVServiceConfiguration) savedConfiguration).getEc2InstanceNames())) {
          updateOperations.unset("ec2InstanceName").unset("ec2Metrics");
        }
        if (isNotEmpty(cloudWatchCVServiceConfiguration.getLambdaFunctionsMetrics())) {
          updateOperations.set("lambdaFunctionsMetrics", cloudWatchCVServiceConfiguration.getLambdaFunctionsMetrics());
        } else if (isNotEmpty(((CloudWatchCVServiceConfiguration) savedConfiguration).getLambdaFunctionsMetrics())) {
          updateOperations.unset("lambdaFunctionsMetrics");
        }
        if (isNotEmpty(cloudWatchCVServiceConfiguration.getEcsMetrics())) {
          updateOperations.set("ecsMetrics", cloudWatchCVServiceConfiguration.getEcsMetrics());
        } else if (isNotEmpty(((CloudWatchCVServiceConfiguration) savedConfiguration).getEcsMetrics())) {
          updateOperations.unset("ecsMetrics");
        }
        break;
      case SUMO:
        LogsCVConfiguration logsCVConfiguration = (LogsCVConfiguration) cvConfiguration;
        updateOperations.set("query", logsCVConfiguration.getQuery())
            .set("baselineStartMinute", logsCVConfiguration.getBaselineStartMinute())
            .set("baselineEndMinute", logsCVConfiguration.getBaselineEndMinute());

        resetBaselineIfNecessary(logsCVConfiguration, (LogsCVConfiguration) savedConfiguration);
        break;
      case ELK:
        ElkCVConfiguration elkCVConfiguration = (ElkCVConfiguration) cvConfiguration;
        updateOperations.set("query", elkCVConfiguration.getQuery())
            .set("baselineStartMinute", elkCVConfiguration.getBaselineStartMinute())
            .set("baselineEndMinute", elkCVConfiguration.getBaselineEndMinute())
            .set("queryType", elkCVConfiguration.getQueryType())
            .set("index", elkCVConfiguration.getIndex())
            .set("hostnameField", elkCVConfiguration.getHostnameField())
            .set("messageField", elkCVConfiguration.getMessageField())
            .set("timestampField", elkCVConfiguration.getTimestampField())
            .set("timestampFormat", elkCVConfiguration.getTimestampFormat());

        resetBaselineIfNecessary(elkCVConfiguration, (LogsCVConfiguration) savedConfiguration);
        break;
      case BUG_SNAG:
        BugsnagCVConfiguration bugsnagCVConfiguration = (BugsnagCVConfiguration) cvConfiguration;
        updateOperations.set("query", bugsnagCVConfiguration.getQuery())
            .set("orgId", bugsnagCVConfiguration.getOrgId())
            .set("projectId", bugsnagCVConfiguration.getProjectId())
            .set("browserApplication", bugsnagCVConfiguration.isBrowserApplication());
        if (isNotEmpty(bugsnagCVConfiguration.getReleaseStage())) {
          updateOperations.set("releaseStage", bugsnagCVConfiguration.getReleaseStage());
        } else if (isNotEmpty(((BugsnagCVConfiguration) savedConfiguration).getReleaseStage())) {
          updateOperations.unset("releaseStage");
        }
        break;
      default:
        throw new IllegalStateException("Invalid state type: " + stateType);
    }

    return updateOperations;
  }

  private void resetBaselineIfNecessary(
      LogsCVConfiguration updatedLogsCVConfiguration, LogsCVConfiguration savedLogsCVConfiguration) {
    if (savedLogsCVConfiguration.getBaselineStartMinute() != updatedLogsCVConfiguration.getBaselineStartMinute()
        || savedLogsCVConfiguration.getBaselineEndMinute() != updatedLogsCVConfiguration.getBaselineEndMinute()) {
      logger.info("recalibrating baseline from {}, to {}", savedLogsCVConfiguration, updatedLogsCVConfiguration);
      resetBaseline(
          savedLogsCVConfiguration.getAppId(), savedLogsCVConfiguration.getUuid(), updatedLogsCVConfiguration);
    }
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
        DatadogCVServiceConfiguration datadogCVServiceConfiguration = (DatadogCVServiceConfiguration) cvConfiguration;
        List<String> metricNames = isNotEmpty(datadogCVServiceConfiguration.getMetrics())
            ? Arrays.asList(datadogCVServiceConfiguration.getMetrics().split(","))
            : new ArrayList<>();
        metricTemplates = DatadogState.metricDefinitions(
            DatadogState.metrics(metricNames, datadogCVServiceConfiguration.getDatadogServiceName()).values());
        metricTemplate = TimeSeriesMetricTemplates.builder()
                             .stateType(stateType)
                             .metricTemplates(metricTemplates)
                             .cvConfigId(cvConfiguration.getUuid())
                             .build();
        break;
      case CLOUD_WATCH:
        metricTemplates = CloudWatchState.fetchMetricTemplates(
            CloudWatchServiceImpl.fetchMetrics((CloudWatchCVServiceConfiguration) cvConfiguration));
        metricTemplate = TimeSeriesMetricTemplates.builder()
                             .stateType(stateType)
                             .metricTemplates(metricTemplates)
                             .cvConfigId(cvConfiguration.getUuid())
                             .build();
        break;

      case SUMO:
      case ELK:
      case BUG_SNAG:
        return;

      default:
        throw new WingsException("No matching state type found " + stateType);
    }
    metricTemplate.setAppId(appId);
    metricTemplate.setAccountId(accountId);
    metricTemplate.setStateExecutionId(CV_24x7_STATE_EXECUTION + "-" + cvConfiguration.getUuid());
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
    wingsPersistence.delete(
        wingsPersistence.createQuery(CVConfiguration.class).filter(CVConfiguration.ACCOUNT_ID_KEY, accountId));
  }

  @Override
  public boolean updateAlertSettings(String cvConfigId, CVConfiguration cvConfiguration) {
    Map<String, Object> updatePairs = new HashMap<>();
    updatePairs.put("alertEnabled", cvConfiguration.isAlertEnabled());
    updatePairs.put("alertThreshold", cvConfiguration.getAlertThreshold());
    wingsPersistence.updateFields(CVConfiguration.class, cvConfigId, updatePairs);
    return true;
  }

  @Override
  public boolean updateSnooze(String cvConfigId, CVConfiguration cvConfiguration) {
    if (cvConfiguration.getSnoozeStartTime() > 0 && cvConfiguration.getSnoozeEndTime() > 0) {
      Preconditions.checkState(cvConfiguration.getSnoozeEndTime() > cvConfiguration.getSnoozeStartTime(),
          "end time should be later than start time, start time:  " + cvConfiguration.getSnoozeStartTime()
              + " endTime: " + cvConfiguration.getSnoozeEndTime());
      Map<String, Object> updatePairs = new HashMap<>();
      updatePairs.put("snoozeStartTime", cvConfiguration.getSnoozeStartTime());
      updatePairs.put("snoozeEndTime", cvConfiguration.getSnoozeEndTime());
      wingsPersistence.updateFields(CVConfiguration.class, cvConfigId, updatePairs);
      return true;
    }
    return false;
  }
}
