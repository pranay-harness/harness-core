package software.wings.service.impl.newrelic;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.exception.WingsException.USER;
import static software.wings.service.impl.ThirdPartyApiCallLog.apiCallLogWithDummyStateExecution;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.serializer.YamlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.APMFetchConfig;
import software.wings.annotation.Encryptable;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.common.Constants;
import software.wings.common.VerificationConstants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.APMDelegateService;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.apm.MLServiceUtil;
import software.wings.service.impl.newrelic.NewRelicApplication.NewRelicApplications;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextFactory;
import software.wings.sm.StateType;
import software.wings.sm.states.NewRelicState;
import software.wings.sm.states.NewRelicState.Metric;
import software.wings.utils.CacheHelper;
import software.wings.utils.Misc;

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

/**
 * Created by rsingh on 8/28/17.
 */
public class NewRelicServiceImpl implements NewRelicService {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicServiceImpl.class);

  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private CacheHelper cacheHelper;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutionContextFactory executionContextFactory;
  @Inject private MLServiceUtil mlServiceUtil;

  @Override
  public void validateAPMConfig(SettingAttribute settingAttribute, APMValidateCollectorConfig config) {
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      delegateProxyFactory.get(APMDelegateService.class, syncTaskContext).validateCollector(config);
    } catch (Exception e) {
      String errorMsg = e.getCause() != null ? Misc.getMessage(e.getCause()) : Misc.getMessage(e);
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
      SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
      return delegateProxyFactory.get(APMDelegateService.class, syncTaskContext).fetch(apmValidateCollectorConfig);
    } catch (Exception e) {
      String errorMsg = e.getCause() != null ? Misc.getMessage(e.getCause()) : Misc.getMessage(e);
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR, USER).addParam("reason", errorMsg);
    }
  }

  @Override
  public void validateConfig(SettingAttribute settingAttribute, StateType stateType) {
    ErrorCode errorCode = null;
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      switch (stateType) {
        case NEW_RELIC:
          errorCode = ErrorCode.NEWRELIC_CONFIGURATION_ERROR;
          delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
              .validateConfig((NewRelicConfig) settingAttribute.getValue());
          break;
        case APP_DYNAMICS:
          errorCode = ErrorCode.APPDYNAMICS_CONFIGURATION_ERROR;
          AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
          delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext).validateConfig(appDynamicsConfig);
          break;
        case DYNA_TRACE:
          errorCode = ErrorCode.DYNA_TRACE_CONFIGURATION_ERROR;
          DynaTraceConfig dynaTraceConfig = (DynaTraceConfig) settingAttribute.getValue();
          delegateProxyFactory.get(DynaTraceDelegateService.class, syncTaskContext).validateConfig(dynaTraceConfig);
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
      throw new WingsException(errorCode, USER).addParam("reason", Misc.getMessage(e));
    }
  }

  @Override
  public List<NewRelicApplication> getApplications(String settingId, StateType stateType) {
    ErrorCode errorCode = null;
    try {
      final SettingAttribute settingAttribute = settingsService.get(settingId);
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      switch (stateType) {
        case NEW_RELIC:
          Cache<String, NewRelicApplications> newRelicApplicationCache = cacheHelper.getNewRelicApplicationCache();
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
          .addParam("message", "Error in getting new relic applications. " + Misc.getMessage(e));
    }
  }

  @Override
  public List<NewRelicApplicationInstance> getApplicationInstances(
      String settingId, long applicationId, StateType stateType) {
    ErrorCode errorCode = null;
    try {
      final SettingAttribute settingAttribute = settingsService.get(settingId);
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = aContext()
                                            .withAccountId(settingAttribute.getAccountId())
                                            .withAppId(Base.GLOBAL_APP_ID)
                                            .withTimeout(Constants.DEFAULT_SYNC_CALL_TIMEOUT * 3)
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
          secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = aContext()
                                            .withAccountId(settingAttribute.getAccountId())
                                            .withAppId(Base.GLOBAL_APP_ID)
                                            .withTimeout(Constants.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                            .build();
      return delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
          .getTxnsWithData((NewRelicConfig) settingAttribute.getValue(), encryptionDetails, applicationId, null);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.NEWRELIC_ERROR, USER)
          .addParam("message", "Error in getting txns with data. " + e.getMessage());
    }
  }

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(
      String settingId, long newRelicApplicationId, long instanceId, long fromTime, long toTime) {
    try {
      final SettingAttribute settingAttribute = settingsService.get(settingId);
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = aContext()
                                            .withAccountId(settingAttribute.getAccountId())
                                            .withAppId(Base.GLOBAL_APP_ID)
                                            .withTimeout(Constants.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                            .build();
      return delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
          .getMetricsWithDataForNode((NewRelicConfig) settingAttribute.getValue(), encryptionDetails,
              newRelicApplicationId, instanceId, fromTime, toTime,
              apiCallLogWithDummyStateExecution(settingAttribute.getAccountId()));
    } catch (Exception e) {
      logger.info("error getting metric data for node", e);
      throw new WingsException(ErrorCode.NEWRELIC_ERROR, USER)
          .addParam("message", "Error in getting metric data for the node. " + e.getMessage());
    }
  }

  @Override
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      NewRelicSetupTestNodeData setupTestNodeData) {
    String hostName = mlServiceUtil.getHostNameFromExpression(setupTestNodeData);
    List<NewRelicApplicationInstance> applicationInstances = getApplicationInstances(
        setupTestNodeData.getSettingId(), setupTestNodeData.getNewRelicAppId(), StateType.NEW_RELIC);
    long instanceId = -1;
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

    if (setupTestNodeData.getToTime() <= 0 || setupTestNodeData.getFromTime() <= 0) {
      setupTestNodeData.setToTime(System.currentTimeMillis());
      setupTestNodeData.setFromTime(setupTestNodeData.getToTime() - TimeUnit.MINUTES.toMillis(15));
    }
    return new RestResponse<>(
        getMetricsWithDataForNode(setupTestNodeData.getSettingId(), setupTestNodeData.getNewRelicAppId(), instanceId,
            setupTestNodeData.getFromTime(), setupTestNodeData.getToTime()));
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
}
