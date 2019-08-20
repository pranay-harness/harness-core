package software.wings.service.impl.newrelic;

import static io.harness.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.APMFetchConfig;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.common.VerificationConstants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.APMDelegateService;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.apm.MLServiceUtils;
import software.wings.service.impl.newrelic.NewRelicApplication.NewRelicApplications;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextFactory;
import software.wings.sm.StateType;
import software.wings.sm.states.APMVerificationState.Method;
import software.wings.sm.states.NewRelicState;
import software.wings.sm.states.NewRelicState.Metric;
import software.wings.utils.CacheManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.cache.Cache;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 8/28/17.
 */
@Singleton
@Slf4j
public class NewRelicServiceImpl implements NewRelicService {
  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private CacheManager cacheManager;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutionContextFactory executionContextFactory;
  @Inject private MLServiceUtils mlServiceUtils;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public void validateAPMConfig(SettingAttribute settingAttribute, APMValidateCollectorConfig config) {
    try {
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(settingAttribute.getAccountId())
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                            .build();
      delegateProxyFactory.get(APMDelegateService.class, syncTaskContext).validateCollector(config);
    } catch (Exception e) {
      String errorMsg = e.getCause() != null ? ExceptionUtils.getMessage(e.getCause()) : ExceptionUtils.getMessage(e);
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR, USER).addParam("reason", errorMsg);
    }
  }

  @Override
  public String fetch(String accountId, String serverConfigId, APMFetchConfig fetchConfig) {
    try {
      SettingAttribute settingAttribute = settingsService.get(serverConfigId);
      APMVerificationConfig apmVerificationConfig = (APMVerificationConfig) settingAttribute.getValue();
      APMValidateCollectorConfig apmValidateCollectorConfig =
          APMValidateCollectorConfig.builder()
              .baseUrl(apmVerificationConfig.getUrl())
              .headers(apmVerificationConfig.collectionHeaders())
              .options(apmVerificationConfig.collectionParams())
              .url(fetchConfig.getUrl())
              .body(fetchConfig.getBody())
              .encryptedDataDetails(apmVerificationConfig.encryptedDataDetails(secretManager))
              .build();
      if (isNotEmpty(fetchConfig.getBody())) {
        apmValidateCollectorConfig.setCollectionMethod(Method.POST);
      }

      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(accountId)
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                            .build();
      return delegateProxyFactory.get(APMDelegateService.class, syncTaskContext)
          .fetch(apmValidateCollectorConfig, ThirdPartyApiCallLog.createApiCallLog(accountId, fetchConfig.getGuid()));
    } catch (Exception e) {
      String errorMsg = e.getCause() != null ? ExceptionUtils.getMessage(e.getCause()) : ExceptionUtils.getMessage(e);
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR, USER).addParam("reason", errorMsg);
    }
  }

  @Override
  public void validateConfig(
      SettingAttribute settingAttribute, StateType stateType, List<EncryptedDataDetail> encryptedDataDetails) {
    ErrorCode errorCode = null;
    try {
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(settingAttribute.getAccountId())
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                            .build();
      switch (stateType) {
        case NEW_RELIC:
          errorCode = ErrorCode.NEWRELIC_CONFIGURATION_ERROR;
          delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
              .validateConfig((NewRelicConfig) settingAttribute.getValue(), encryptedDataDetails);
          break;
        case APP_DYNAMICS:
          errorCode = ErrorCode.APPDYNAMICS_CONFIGURATION_ERROR;
          AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
          delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
              .validateConfig(appDynamicsConfig, encryptedDataDetails);
          break;
        case DYNA_TRACE:
          errorCode = ErrorCode.DYNA_TRACE_CONFIGURATION_ERROR;
          DynaTraceConfig dynaTraceConfig = (DynaTraceConfig) settingAttribute.getValue();
          delegateProxyFactory.get(DynaTraceDelegateService.class, syncTaskContext)
              .validateConfig(dynaTraceConfig, encryptedDataDetails);
          break;
        case PROMETHEUS:
          errorCode = ErrorCode.PROMETHEUS_CONFIGURATION_ERROR;
          PrometheusConfig prometheusConfig = (PrometheusConfig) settingAttribute.getValue();
          delegateProxyFactory.get(PrometheusDelegateService.class, syncTaskContext).validateConfig(prometheusConfig);
          break;
        default:
          throw new IllegalStateException("Invalid state" + stateType);
      }
    } catch (Exception e) {
      throw new WingsException(errorCode, USER, e).addParam("reason", ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public List<NewRelicApplication> getApplications(String settingId, StateType stateType) {
    ErrorCode errorCode = null;
    try {
      final SettingAttribute settingAttribute = settingsService.get(settingId);
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(settingAttribute.getAccountId())
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                            .build();
      switch (stateType) {
        case NEW_RELIC:
          Cache<String, NewRelicApplications> newRelicApplicationCache = cacheManager.getNewRelicApplicationCache();
          String key = settingAttribute.getUuid();
          NewRelicApplications applications;
          try {
            applications = newRelicApplicationCache.get(key);
            if (applications != null) {
              return applications.getApplications();
            }
          } catch (Exception ex) {
            // If there was any exception, remove that entry from cache
            newRelicApplicationCache.remove(key);
          }

          errorCode = ErrorCode.NEWRELIC_ERROR;
          List<NewRelicApplication> allApplications =
              delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
                  .getAllApplications((NewRelicConfig) settingAttribute.getValue(), encryptionDetails, null);
          // check if number of apps is too big to compute
          if (isNotEmpty(allApplications) && allApplications.size() == 1 && allApplications.get(0).getId() == -1) {
            return allApplications;
          }
          applications = NewRelicApplications.builder().applications(allApplications).build();
          newRelicApplicationCache.put(key, applications);
          return allApplications;
        case APP_DYNAMICS:
          errorCode = ErrorCode.APPDYNAMICS_ERROR;
          return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
              .getAllApplications((AppDynamicsConfig) settingAttribute.getValue(), encryptionDetails);
        default:
          throw new IllegalStateException("Invalid state" + stateType);
      }

    } catch (Exception e) {
      throw new WingsException(errorCode, USER)
          .addParam("message", "Error in getting new relic applications. " + ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public List<NewRelicApplicationInstance> getApplicationInstances(
      String settingId, long applicationId, StateType stateType) {
    ErrorCode errorCode = null;
    try {
      final SettingAttribute settingAttribute = settingsService.get(settingId);
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(settingAttribute.getAccountId())
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                            .build();
      switch (stateType) {
        case NEW_RELIC:
          errorCode = ErrorCode.NEWRELIC_ERROR;
          return delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
              .getApplicationInstances(
                  (NewRelicConfig) settingAttribute.getValue(), encryptionDetails, applicationId, null);
        default:
          throw new IllegalStateException("Invalid state" + stateType);
      }

    } catch (Exception e) {
      throw new WingsException(errorCode, USER)
          .addParam("message", "Error in getting new relic applications. " + e.getMessage());
    }
  }

  @Override
  public List<NewRelicMetric> getTxnsWithData(String settingId, long applicationId, long instanceId) {
    try {
      final SettingAttribute settingAttribute = settingsService.get(settingId);
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(settingAttribute.getAccountId())
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                            .build();
      return delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
          .getTxnsWithData((NewRelicConfig) settingAttribute.getValue(), encryptionDetails, applicationId,
              featureFlagService.isEnabled(
                  FeatureName.DISABLE_METRIC_NAME_CURLY_BRACE_CHECK, settingAttribute.getAccountId()),
              null);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.NEWRELIC_ERROR, USER)
          .addParam("message", "Error in getting txns with data. " + e.getMessage());
    }
  }

  @Override
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      NewRelicSetupTestNodeData setupTestNodeData) {
    String hostName;
    long instanceId = -1;
    // check if it is for service level, serviceId is empty then get hostname
    if (!setupTestNodeData.isServiceLevel()) {
      hostName = mlServiceUtils.getHostNameFromExpression(setupTestNodeData);
      List<NewRelicApplicationInstance> applicationInstances = getApplicationInstances(
          setupTestNodeData.getSettingId(), setupTestNodeData.getNewRelicAppId(), StateType.NEW_RELIC);

      for (NewRelicApplicationInstance applicationInstance : applicationInstances) {
        if (applicationInstance.getHost().equals(hostName)) {
          instanceId = applicationInstance.getId();
          break;
        }
      }

      if (instanceId == -1) {
        throw new WingsException(ErrorCode.NEWRELIC_CONFIGURATION_ERROR, USER)
            .addParam("reason", "No node with name " + hostName + " found reporting to new relic");
      }
    }

    setupTestNodeData.setToTime(System.currentTimeMillis());
    setupTestNodeData.setFromTime(setupTestNodeData.getToTime() - TimeUnit.MINUTES.toMillis(15));
    return new RestResponse<>(getMetricsWithDataForNode(setupTestNodeData, instanceId));
  }

  private VerificationNodeDataSetupResponse getMetricsWithDataForNode(
      NewRelicSetupTestNodeData setupTestNodeData, long instanceId) {
    try {
      final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(settingAttribute.getAccountId())
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                            .build();
      return delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
          .getMetricsWithDataForNode((NewRelicConfig) settingAttribute.getValue(), encryptionDetails, setupTestNodeData,
              instanceId,
              !featureFlagService.isEnabled(
                  FeatureName.DISABLE_METRIC_NAME_CURLY_BRACE_CHECK, settingAttribute.getAccountId()),
              createApiCallLog(settingAttribute.getAccountId(), setupTestNodeData.getGuid()));
    } catch (Exception e) {
      logger.info("error getting metric data for node", e);
      throw new WingsException(ErrorCode.NEWRELIC_ERROR, USER)
          .addParam("message", "Error in getting metric data for the node. " + e.getMessage());
    }
  }

  public Map<String, TimeSeriesMetricDefinition> metricDefinitions(Collection<Metric> metrics) {
    Map<String, TimeSeriesMetricDefinition> metricDefinitionByName = new HashMap<>();
    for (Metric metric : metrics) {
      metricDefinitionByName.put(metric.getMetricName(),
          TimeSeriesMetricDefinition.builder()
              .metricName(metric.getMetricName())
              .metricType(metric.getMlMetricType())
              .build());
    }
    return metricDefinitionByName;
  }

  /**
   *
   * @param yamlPath - String containing path from rest/src/main/java/resources
   *                   e.g. Path to new relic metrics yaml => /apm/newrelic_metrics.yml
   * @return Mapping of name of the group of metrics (e.g. WebTransactions) to List of Metric Objects
   * @throws WingsException
   */
  private Map<String, List<Metric>> getMetricsFromYaml(String yamlPath) throws WingsException {
    YamlUtils yamlUtils = new YamlUtils();
    URL url = NewRelicState.class.getResource(yamlPath);
    try {
      String yaml = Resources.toString(url, Charsets.UTF_8);
      return yamlUtils.read(yaml, new TypeReference<Map<String, List<Metric>>>() {});
    } catch (IOException ioex) {
      logger.error("Could not read " + yamlPath);
      throw new WingsException("Unable to load New Relic metrics", ioex);
    }
  }

  /**
   * Get a mapping from metric name to {@link Metric} for the list of metric names
   * provided as input.
   * This method is meant to be called before saving a metric template.
   * The output of this method shall be consumed by metricDefinitions(...)
   * @param metricNames - List[String] containing metric names
   * @return - Map[String, Metric], a mapping from metric name to {@link Metric}
   */
  public Map<String, Metric> getMetricsCorrespondingToMetricNames(List<String> metricNames) {
    Map<String, Metric> metricMap = new HashMap<>();
    try {
      Map<String, List<Metric>> metrics = getMetricsFromYaml(VerificationConstants.getNewRelicMetricsYamlUrl());
      if (metrics == null) {
        return metricMap;
      }

      Set<String> metricNamesSet = metricNames == null ? new HashSet<>() : Sets.newHashSet(metricNames);

      // Iterate over the metrics present in the YAML file
      for (Map.Entry<String, List<Metric>> entry : metrics.entrySet()) {
        if (entry == null) {
          logger.error("Found a null entry in the NewRelic Metrics YAML file.");
        } else {
          entry.getValue().forEach(metric -> {
            /*
            We consider 2 cases:
            1. metricNames is empty - we add all metrics present in the YAML to the metricMap in this case
            2. metricNames is non-empty - we only add metrics which are present in the list
             */
            if (metric != null && (isEmpty(metricNames) || metricNamesSet.contains(metric.getMetricName()))) {
              if (metric.getTags() == null) {
                metric.setTags(new HashSet<>());
              }
              // Add top-level key of the YAML as a tag
              metric.getTags().add(entry.getKey());
              metricMap.put(metric.getMetricName(), metric);
            }
          });
        }
      }

      /*
      If metricNames is non-empty but metricMap is, it means that all
      metric names were spelt incorrectly.
       */
      if (!isEmpty(metricNames) && metricMap.isEmpty()) {
        logger.warn("Incorrect set of metric names received. Maybe the UI is sending incorrect metric names.");
        throw new WingsException("Incorrect Metric Names received.");
      }

      return metricMap;
    } catch (WingsException wex) {
      // Return empty metricMap
      return metricMap;
    }
  }

  /**
   * Read the YAML file containing New Relic's Metric Information
   * and return the metrics as a list.
   * @return List[Metric]
   */
  public List<Metric> getListOfMetrics() {
    try {
      Map<String, List<Metric>> metricsMap = getMetricsFromYaml(VerificationConstants.getNewRelicMetricsYamlUrl());
      if (metricsMap == null) {
        logger.error("Metric Map read from new relic metrics YAML is null. This is unexpected behaviour. "
            + "Probably the path to the YAML is incorrect.");
        return new ArrayList<>();
      }
      return metricsMap.values().stream().flatMap(metric -> metric.stream()).collect(Collectors.toList());
    } catch (Exception ex) {
      throw new WingsException("Unable to load New Relic metrics", ex);
    }
  }

  @Override
  public NewRelicApplication resolveApplicationName(
      @NotNull String settingId, @NotNull String newRelicApplicationName) {
    ErrorCode errorCode = null;
    try {
      final SettingAttribute settingAttribute = settingsService.get(settingId);
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(settingAttribute.getAccountId())
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                            .build();
      errorCode = ErrorCode.NEWRELIC_ERROR;
      return delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
          .resolveNewRelicApplicationName(
              (NewRelicConfig) settingAttribute.getValue(), encryptionDetails, newRelicApplicationName, null);
    } catch (Exception e) {
      throw new WingsException(errorCode, USER)
          .addParam("message",
              "Error in resolving newrelic application " + newRelicApplicationName + " . "
                  + ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public NewRelicApplication resolveApplicationId(@NotNull String settingId, @NotNull String newRelicApplicationId) {
    ErrorCode errorCode = null;
    try {
      final SettingAttribute settingAttribute = settingsService.get(settingId);
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(settingAttribute.getAccountId())
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                            .build();
      errorCode = ErrorCode.NEWRELIC_ERROR;
      return delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
          .resolveNewRelicApplicationId(
              (NewRelicConfig) settingAttribute.getValue(), encryptionDetails, newRelicApplicationId, null);
    } catch (Exception e) {
      throw new WingsException(errorCode, USER)
          .addParam("message",
              "Error in resolving newrelic application with ID: " + newRelicApplicationId + " . "
                  + ExceptionUtils.getMessage(e));
    }
  }
}
