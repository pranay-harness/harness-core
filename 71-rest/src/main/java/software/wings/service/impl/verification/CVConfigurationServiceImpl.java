package software.wings.service.impl.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;
import static software.wings.sm.StateType.STACK_DRIVER_LOG;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.CloudWatchServiceImpl;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogDataRecord.LogDataRecordKeys;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates.TimeSeriesMetricTemplatesKeys;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask.LearningEngineAnalysisTaskKeys;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.StateType;
import software.wings.sm.states.CloudWatchState;
import software.wings.sm.states.DatadogState;
import software.wings.sm.states.PrometheusState;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;
import software.wings.verification.apm.APMCVServiceConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration;
import software.wings.verification.log.BugsnagCVConfiguration;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.StackdriverCVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Vaibhav Tulsyan
 * 09/Oct/2018
 */
@Singleton
@Slf4j
public class CVConfigurationServiceImpl implements CVConfigurationService {
  @Inject WingsPersistence wingsPersistence;
  @Inject CvValidationService cvValidationService;
  @Inject private YamlPushService yamlPushService;

  @Override
  public String saveConfiguration(String accountId, String appId, StateType stateType, Object params) {
    return saveConfiguration(accountId, appId, stateType, params, false);
  }

  @Override
  public String saveConfiguration(
      String accountId, String appId, StateType stateType, Object params, boolean createdFromYaml) {
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
        DatadogCVServiceConfiguration ddCVConfig = (DatadogCVServiceConfiguration) cvConfiguration;
        if (isEmpty(ddCVConfig.getDatadogServiceName()) && isEmpty(ddCVConfig.getDockerMetrics())
            && isEmpty(ddCVConfig.getEcsMetrics()) && isEmpty(ddCVConfig.getCustomMetrics())) {
          throw new WingsException("No metrics found in the yaml");
        }
        break;

      case CLOUD_WATCH:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), CloudWatchCVServiceConfiguration.class);
        CloudWatchCVServiceConfiguration cloudWatchCVServiceConfiguration =
            (CloudWatchCVServiceConfiguration) cvConfiguration;
        if (isEmpty(cloudWatchCVServiceConfiguration.getLoadBalancerMetrics())
            && isEmpty(cloudWatchCVServiceConfiguration.getEc2InstanceNames())
            && isEmpty(cloudWatchCVServiceConfiguration.getLambdaFunctionsMetrics())
            && isEmpty(cloudWatchCVServiceConfiguration.getEcsMetrics())) {
          throw new WingsException("No metric provided in Configuration");
        }
        break;

      case SUMO:
      case DATA_DOG_LOG:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), LogsCVConfiguration.class);
        break;

      case ELK:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), ElkCVConfiguration.class);
        ElkCVConfiguration elkCVConfiguration = (ElkCVConfiguration) cvConfiguration;
        cvValidationService.validateELKQuery(accountId, appId, elkCVConfiguration.getConnectorId(),
            elkCVConfiguration.getQuery(), elkCVConfiguration.getIndex(), elkCVConfiguration.getHostnameField(),
            elkCVConfiguration.getMessageField(), elkCVConfiguration.getTimestampField());
        break;
      case BUG_SNAG:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), BugsnagCVConfiguration.class);
        break;

      case STACK_DRIVER_LOG:
        cvConfiguration = JsonUtils.asObject(JsonUtils.asJson(params), StackdriverCVConfiguration.class);
        StackdriverCVConfiguration stackdriverCVConfiguration = (StackdriverCVConfiguration) cvConfiguration;
        if (stackdriverCVConfiguration.isLogsConfiguration()) {
          stackdriverCVConfiguration.setStateType(STACK_DRIVER_LOG);
          if (!cvValidationService.validateStackdriverQuery(accountId, appId,
                  stackdriverCVConfiguration.getConnectorId(), stackdriverCVConfiguration.getQuery())) {
            throw new WingsException(
                "Invalid Query, Please provide textPayload in query " + stackdriverCVConfiguration.getQuery());
          }
        }
        break;

      default:
        throw new WingsException("No matching state type found " + stateType);
    }

    cvConfiguration.setAccountId(accountId);
    cvConfiguration.setAppId(appId);
    cvConfiguration.setStateType(stateType);
    cvConfiguration.setUuid(generateUuid());
    return saveToDatabase(cvConfiguration, createdFromYaml).getUuid();
  }

  public CVConfiguration saveToDatabase(CVConfiguration cvConfiguration, boolean createdFromYaml) {
    try {
      saveMetricTemplate(
          cvConfiguration.getAppId(), cvConfiguration.getAccountId(), cvConfiguration, cvConfiguration.getStateType());

      CVConfiguration configuration = wingsPersistence.saveAndGet(CVConfiguration.class, cvConfiguration);
      if (!createdFromYaml) {
        yamlPushService.pushYamlChangeSet(
            cvConfiguration.getAccountId(), null, cvConfiguration, Type.CREATE, cvConfiguration.isSyncFromGit(), false);
      }
      return configuration;
    } catch (DuplicateKeyException ex) {
      throw new WingsException("A Service Verification with the name " + cvConfiguration.getName()
          + " already exists. Please choose a unique name.");
    }
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
                                          .filter(CVConfigurationKeys.name, name)
                                          .filter("appId", appId)
                                          .filter(CVConfigurationKeys.envId, envId)
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

  @Override
  public <T extends CVConfiguration> List<T> listConfigurations(
      String accountId, String appId, String envId, StateType stateType) {
    Query<T> configurationQuery = (Query<T>) wingsPersistence.createQuery(CVConfiguration.class)
                                      .filter(CVConfigurationKeys.accountId, accountId)
                                      .filter("appId", appId)
                                      .filter(CVConfigurationKeys.isWorkflowConfig, false);
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
  public <T extends CVConfiguration> List<T> listConfigurations(
      String accountId, List<String> appIds, List<String> envIds) {
    Query<T> configurationQuery = (Query<T>) wingsPersistence.createQuery(CVConfiguration.class)
                                      .filter(CVConfigurationKeys.accountId, accountId)
                                      .filter(CVConfigurationKeys.isWorkflowConfig, false);

    if (isNotEmpty(appIds)) {
      configurationQuery = configurationQuery.field(CVConfiguration.APP_ID_KEY).in(appIds);
    }

    if (isNotEmpty(envIds)) {
      configurationQuery = configurationQuery.field(CVConfigurationKeys.envId).in(envIds);
    }

    List<T> rv = new ArrayList<>();
    try (HIterator<T> iterator = new HIterator<>(configurationQuery.fetch())) {
      while (iterator.hasNext()) {
        rv.add(iterator.next());
      }
    }
    return rv;
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
      case DATA_DOG_LOG:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), LogsCVConfiguration.class);
        break;
      case STACK_DRIVER_LOG:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), StackdriverCVConfiguration.class);
        StackdriverCVConfiguration stackdriverCVConfiguration = (StackdriverCVConfiguration) updatedConfig;
        if (stackdriverCVConfiguration.isLogsConfiguration()) {
          updatedConfig.setStateType(STACK_DRIVER_LOG);
          if (!cvValidationService.validateStackdriverQuery(accountId, appId,
                  stackdriverCVConfiguration.getConnectorId(), stackdriverCVConfiguration.getQuery())) {
            throw new WingsException(
                "Invalid Query, Please provide textPayload in query " + stackdriverCVConfiguration.getQuery());
          }
        }
        break;
      case ELK:
        updatedConfig = JsonUtils.asObject(JsonUtils.asJson(params), ElkCVConfiguration.class);
        ElkCVConfiguration elkCVConfiguration = (ElkCVConfiguration) updatedConfig;
        cvValidationService.validateELKQuery(accountId, appId, elkCVConfiguration.getConnectorId(),
            elkCVConfiguration.getQuery(), elkCVConfiguration.getIndex(), elkCVConfiguration.getHostnameField(),
            elkCVConfiguration.getMessageField(), elkCVConfiguration.getTimestampField());
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
    CVConfiguration newConfiguration = wingsPersistence.get(CVConfiguration.class, savedConfiguration.getUuid());
    yamlPushService.pushYamlChangeSet(accountId, savedConfiguration, newConfiguration, Type.UPDATE, false,
        !savedConfiguration.getName().equals(newConfiguration.getName()));
    return savedConfiguration.getUuid();
  }

  @Override
  public boolean deleteConfiguration(String accountId, String appId, String serviceConfigurationId) {
    return deleteConfiguration(accountId, appId, serviceConfigurationId, false);
  }

  @Override
  public boolean deleteConfiguration(
      String accountId, String appId, String serviceConfigurationId, boolean isSyncFromGit) {
    Object savedConfig;
    savedConfig = wingsPersistence.get(CVConfiguration.class, serviceConfigurationId);
    if (savedConfig == null) {
      return false;
    }
    wingsPersistence.delete(CVConfiguration.class, serviceConfigurationId);
    deleteTemplate(accountId, serviceConfigurationId, ((CVConfiguration) savedConfig).getStateType());

    yamlPushService.pushYamlChangeSet(accountId, savedConfig, null, Type.DELETE, isSyncFromGit, false);
    return true;
  }

  @Override
  public String resetBaseline(String appId, String cvConfigId, LogsCVConfiguration logsCVConfiguration) {
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
    wingsPersistence.delete(wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                .filter(LogDataRecordKeys.cvConfigId, cvConfigId));
    wingsPersistence.delete(wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority)
                                .filter(LogMLAnalysisRecordKeys.cvConfigId, cvConfigId)
                                .field(LogMLAnalysisRecordKeys.logCollectionMinute)
                                .greaterThanOrEq(logsCVConfiguration.getBaselineStartMinute()));
    wingsPersistence.update(wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority)
                                .filter(LogMLAnalysisRecordKeys.cvConfigId, cvConfigId)
                                .field(LogMLAnalysisRecordKeys.logCollectionMinute)
                                .lessThanOrEq(logsCVConfiguration.getBaselineStartMinute()),
        wingsPersistence.createUpdateOperations(LogMLAnalysisRecord.class)
            .set(LogMLAnalysisRecordKeys.deprecated, true));
    wingsPersistence.delete(wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
                                .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfigId));
    cvConfiguration.setBaselineStartMinute(logsCVConfiguration.getBaselineStartMinute());
    cvConfiguration.setBaselineEndMinute(logsCVConfiguration.getBaselineEndMinute());
    cvConfiguration.setUuid(null);
    deleteConfiguration(logsCVConfiguration.getAccountId(), logsCVConfiguration.getAppId(), cvConfigId);
    final String newCvConfigId = saveConfiguration(
        cvConfiguration.getAccountId(), cvConfiguration.getAppId(), cvConfiguration.getStateType(), cvConfiguration);
    wingsPersistence.update(wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority)
                                .filter(LogMLAnalysisRecordKeys.cvConfigId, cvConfigId),
        wingsPersistence.createUpdateOperations(LogMLAnalysisRecord.class)
            .set(LogMLAnalysisRecordKeys.cvConfigId, newCvConfigId));
    return newCvConfigId;
  }

  private void deleteTemplate(String accountId, String serviceConfigurationId, StateType stateType) {
    if (!stateType.equals(StateType.APP_DYNAMICS) && !stateType.equals(StateType.NEW_RELIC)) {
      TimeSeriesMetricTemplates timeSeriesMetricTemplates =
          wingsPersistence.createQuery(TimeSeriesMetricTemplates.class)
              .filter(TimeSeriesMetricTemplatesKeys.cvConfigId, serviceConfigurationId)
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
        updateOperations.set("applicationId", ((NewRelicCVServiceConfiguration) cvConfiguration).getApplicationId());
        if (isNotEmpty(((NewRelicCVServiceConfiguration) cvConfiguration).getMetrics())) {
          updateOperations.set("metrics", ((NewRelicCVServiceConfiguration) cvConfiguration).getMetrics());
        }
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
        DatadogCVServiceConfiguration datadogCVServiceConfiguration = (DatadogCVServiceConfiguration) cvConfiguration;
        if (isEmpty(datadogCVServiceConfiguration.getDockerMetrics())
            && isEmpty(datadogCVServiceConfiguration.getEcsMetrics())
            && isEmpty(datadogCVServiceConfiguration.getCustomMetrics())
            && isEmpty(datadogCVServiceConfiguration.getDatadogServiceName())) {
          throw new WingsException("No metric provided in Configuration for configId " + savedConfiguration.getUuid()
              + " and serviceId " + savedConfiguration.getServiceId());
        }
        if (isNotEmpty(datadogCVServiceConfiguration.getDatadogServiceName())) {
          updateOperations.set("datadogServiceName", datadogCVServiceConfiguration.getDatadogServiceName());
        } else {
          updateOperations.unset("datadogServiceName");
        }
        if (isNotEmpty(datadogCVServiceConfiguration.getDockerMetrics())) {
          updateOperations.set("dockerMetrics", datadogCVServiceConfiguration.getDockerMetrics());
        } else {
          updateOperations.unset("dockerMetrics");
        }
        if (isNotEmpty(datadogCVServiceConfiguration.getEcsMetrics())) {
          updateOperations.set("ecsMetrics", datadogCVServiceConfiguration.getEcsMetrics());
        } else {
          updateOperations.unset("ecsMetrics");
        }
        if (isNotEmpty(datadogCVServiceConfiguration.getCustomMetrics())) {
          updateOperations.set("customMetrics", datadogCVServiceConfiguration.getCustomMetrics());
        } else {
          updateOperations.unset("customMetrics");
        }
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
      case DATA_DOG_LOG:
        LogsCVConfiguration logsCVConfiguration = (LogsCVConfiguration) cvConfiguration;
        updateOperations.set("query", logsCVConfiguration.getQuery())
            .set("baselineStartMinute", logsCVConfiguration.getBaselineStartMinute())
            .set("baselineEndMinute", logsCVConfiguration.getBaselineEndMinute());
        resetBaselineIfNecessary(logsCVConfiguration, (LogsCVConfiguration) savedConfiguration);
        break;
      case STACK_DRIVER_LOG:
        StackdriverCVConfiguration stackdriverCVConfiguration = (StackdriverCVConfiguration) cvConfiguration;
        updateOperations.set("query", stackdriverCVConfiguration.getQuery())
            .set("isLogsConfiguration", stackdriverCVConfiguration.isLogsConfiguration())
            .set("baselineStartMinute", stackdriverCVConfiguration.getBaselineStartMinute())
            .set("baselineEndMinute", stackdriverCVConfiguration.getBaselineEndMinute());
        resetBaselineIfNecessary(stackdriverCVConfiguration, (StackdriverCVConfiguration) savedConfiguration);
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
            .set("baselineStartMinute", bugsnagCVConfiguration.getBaselineStartMinute())
            .set("baselineEndMinute", bugsnagCVConfiguration.getBaselineEndMinute())
            .set("orgId", bugsnagCVConfiguration.getOrgId())
            .set("projectId", bugsnagCVConfiguration.getProjectId())
            .set("browserApplication", bugsnagCVConfiguration.isBrowserApplication());
        if (isNotEmpty(bugsnagCVConfiguration.getReleaseStage())) {
          updateOperations.set("releaseStage", bugsnagCVConfiguration.getReleaseStage());
        } else if (isNotEmpty(((BugsnagCVConfiguration) savedConfiguration).getReleaseStage())) {
          updateOperations.unset("releaseStage");
        }

        resetBaselineIfNecessary(bugsnagCVConfiguration, (LogsCVConfiguration) savedConfiguration);
        break;
      case APM_VERIFICATION:
        if (isNotEmpty(((APMCVServiceConfiguration) cvConfiguration).getMetricCollectionInfos())) {
          updateOperations.set(
              "metricCollectionInfos", ((APMCVServiceConfiguration) cvConfiguration).getMetricCollectionInfos());
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
      String newCVConfigId = resetBaseline(
          savedLogsCVConfiguration.getAppId(), savedLogsCVConfiguration.getUuid(), updatedLogsCVConfiguration);
      savedLogsCVConfiguration.setUuid(newCVConfigId);
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

        metricTemplates = new HashMap<>();
        if (isNotEmpty(datadogCVServiceConfiguration.getDockerMetrics())) {
          metricTemplates.putAll(getMetricTemplates(datadogCVServiceConfiguration.getDockerMetrics()));
        }
        if (isNotEmpty(datadogCVServiceConfiguration.getEcsMetrics())) {
          metricTemplates.putAll(getMetricTemplates(datadogCVServiceConfiguration.getEcsMetrics()));
        }

        metricTemplates.putAll(DatadogState.metricDefinitions(
            DatadogState
                .metrics(Optional.empty(), Optional.ofNullable(datadogCVServiceConfiguration.getDatadogServiceName()),
                    Optional.ofNullable(datadogCVServiceConfiguration.getCustomMetrics()), Optional.empty(),
                    Optional.empty())
                .values()));

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
      case STACK_DRIVER:
      case DATA_DOG_LOG:
        return;

      default:
        throw new WingsException("No matching state type found " + stateType);
    }
    metricTemplate.setAppId(appId);
    metricTemplate.setAccountId(accountId);
    metricTemplate.setStateExecutionId(CV_24x7_STATE_EXECUTION + "-" + cvConfiguration.getUuid());
    wingsPersistence.save(metricTemplate);
  }

  private Map<String, TimeSeriesMetricDefinition> getMetricTemplates(Map<String, String> metrics) {
    Map<String, TimeSeriesMetricDefinition> metricTemplates = new HashMap<>();
    for (Entry<String, String> entry : metrics.entrySet()) {
      List<String> metricNames =
          Arrays.asList(entry.getValue().split(",")).parallelStream().map(String::trim).collect(Collectors.toList());
      metricTemplates.putAll(
          DatadogState.metricDefinitions(DatadogState
                                             .metrics(Optional.of(metricNames), Optional.empty(), Optional.empty(),
                                                 Optional.of(entry.getKey()), Optional.empty())
                                             .values()));
    }
    return metricTemplates;
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
