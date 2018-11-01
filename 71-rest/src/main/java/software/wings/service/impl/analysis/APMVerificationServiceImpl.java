package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.states.DatadogState.metricEndpointsInfo;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.APMFetchConfig;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Base;
import software.wings.beans.DatadogConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.impl.datadog.DataDogFetchConfig;
import software.wings.service.impl.dynatrace.DynaTraceDataCollectionInfo;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.prometheus.PrometheusDataCollectionInfo;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.analysis.APMVerificationService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.StateType;
import software.wings.sm.states.DynatraceState;
import software.wings.utils.Misc;
import software.wings.verification.CVConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * @author Praveen 9/6/18
 */

public class APMVerificationServiceImpl implements APMVerificationService {
  private static final Logger logger = LoggerFactory.getLogger(APMVerificationServiceImpl.class);
  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EncryptionService encryptionService;

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(
      String accountId, String serverConfigId, Object fetchConfig, StateType type) {
    try {
      if (isEmpty(serverConfigId) || fetchConfig == null) {
        throw new WingsException("Invalid Parameters passed while trying to get test data for APM");
      }
      SettingAttribute settingAttribute = settingsService.get(serverConfigId);
      APMValidateCollectorConfig apmValidateCollectorConfig;
      switch (type) {
        case DATA_DOG:
          DataDogFetchConfig config = (DataDogFetchConfig) fetchConfig;
          DatadogConfig datadogConfig = (DatadogConfig) settingAttribute.getValue();
          List<EncryptedDataDetail> encryptedDataDetails =
              secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);

          apmValidateCollectorConfig = datadogConfig.createAPMValidateCollectorConfig();
          apmValidateCollectorConfig.setEncryptedDataDetails(encryptedDataDetails);
          apmValidateCollectorConfig.getOptions().put("from", String.valueOf(config.getFromtime()));
          apmValidateCollectorConfig.getOptions().put("to", String.valueOf(config.getToTime()));

          Map<String, List<APMMetricInfo>> metricInfoByQuery =
              metricEndpointsInfo(config.getDatadogServiceName(), Arrays.asList(config.getMetrics().split(",")));
          List<Object> loadResponse = new ArrayList<>();

          // loop for each metric
          for (Entry<String, List<APMMetricInfo>> entry : metricInfoByQuery.entrySet()) {
            String url = entry.getKey();
            if (url.contains("${host}")) {
              url = url.replace("${host}", config.getHostName());
            }
            apmValidateCollectorConfig.setUrl(url);
            VerificationNodeDataSetupResponse verificationNodeDataSetupResponse =
                getVerificationNodeDataResponse(accountId, apmValidateCollectorConfig);
            if (!verificationNodeDataSetupResponse.isProviderReachable()) {
              // if not reachable then directly return. no need to process further
              return VerificationNodeDataSetupResponse.builder().providerReachable(false).build();
            }
            // add load response only for metrics containing nodedata.
            if (verificationNodeDataSetupResponse.getLoadResponse().isLoadPresent()) {
              loadResponse.add(verificationNodeDataSetupResponse);
            }
          }

          VerificationLoadResponse response = VerificationLoadResponse.builder()
                                                  .loadResponse(loadResponse)
                                                  .isLoadPresent(!isEmpty(loadResponse))
                                                  .build();

          return VerificationNodeDataSetupResponse.builder()
              .providerReachable(true)
              .loadResponse(response)
              .dataForNode(loadResponse)
              .build();

        case APM_VERIFICATION:
          APMFetchConfig apmFetchConfig = (APMFetchConfig) fetchConfig;
          APMVerificationConfig apmVerificationConfig = (APMVerificationConfig) settingAttribute.getValue();
          apmValidateCollectorConfig =
              APMValidateCollectorConfig.builder()
                  .baseUrl(apmVerificationConfig.getUrl())
                  .headers(apmVerificationConfig.collectionHeaders())
                  .options(apmVerificationConfig.collectionParams())
                  .url(apmFetchConfig.getUrl())
                  .body(apmFetchConfig.getBody())
                  .encryptedDataDetails(apmVerificationConfig.encryptedDataDetails(secretManager))
                  .build();

          return getVerificationNodeDataResponse(accountId, apmValidateCollectorConfig);
        default:
          throw new WingsException("Invalid StateType provided" + type);
      }
    } catch (Exception e) {
      String errorMsg = e.getCause() != null ? Misc.getMessage(e.getCause()) : Misc.getMessage(e);
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR, USER).addParam("reason", errorMsg);
    }
  }

  @Override
  public boolean sendNotifyForMetricAnalysis(String correlationId, MetricDataAnalysisResponse response) {
    try {
      waitNotifyEngine.notify(correlationId, response);
      return true;
    } catch (Exception ex) {
      logger.error("Exception while notifying correlationId {}", correlationId, ex);
      return false;
    }
  }

  @Override
  public boolean collect247Data(String cvConfigId, StateType stateType, long startTime, long endTime) {
    String waitId = generateUuid();
    DelegateTask task;
    switch (stateType) {
      case APP_DYNAMICS:
        AppDynamicsCVServiceConfiguration config =
            (AppDynamicsCVServiceConfiguration) wingsPersistence.createQuery(CVConfiguration.class)
                .filter("_id", cvConfigId)
                .get();

        task = createAppDynamicsDelegateTask(config, waitId, startTime, endTime);
        break;
      case NEW_RELIC:
        NewRelicCVServiceConfiguration nrConfig =
            (NewRelicCVServiceConfiguration) wingsPersistence.createQuery(CVConfiguration.class)
                .filter("_id", cvConfigId)
                .get();
        task = createNewRelicDelegateTask(nrConfig, waitId, startTime, endTime);
        break;
      case DYNA_TRACE:
        DynaTraceCVServiceConfiguration dynaTraceCVServiceConfiguration =
            (DynaTraceCVServiceConfiguration) wingsPersistence.createQuery(CVConfiguration.class)
                .filter("_id", cvConfigId)
                .get();
        task = createDynaTraceDelegateTask(dynaTraceCVServiceConfiguration, waitId, startTime, endTime);
        break;
      case PROMETHEUS:
        PrometheusCVServiceConfiguration prometheusCVServiceConfiguration =
            (PrometheusCVServiceConfiguration) wingsPersistence.createQuery(CVConfiguration.class)
                .filter("_id", cvConfigId)
                .get();
        task = createPrometheusDelegateTask(prometheusCVServiceConfiguration, waitId, startTime, endTime);
        break;
      default:
        logger.error("Calling collect 24x7 data for an unsupported state");
        return false;
    }
    waitNotifyEngine.waitForAll(
        new DataCollectionCallback(null, null, false), waitId); // TODO: is passing nulls here, okay?
    logger.info("Queuing 24x7 data collection task for {}, cvConfigurationId: {}", stateType, cvConfigId);
    delegateService.queueTask(task);
    return true;
  }

  private DelegateTask createDynaTraceDelegateTask(
      DynaTraceCVServiceConfiguration config, String waitId, long startTime, long endTime) {
    DynaTraceConfig dynaTraceConfig = (DynaTraceConfig) settingsService.get(config.getConnectorId()).getValue();
    int timeDuration = (int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
    final DynaTraceDataCollectionInfo dataCollectionInfo =
        DynaTraceDataCollectionInfo.builder()
            .dynaTraceConfig(dynaTraceConfig)
            .applicationId(config.getAppId())
            .serviceId(config.getServiceId())
            .cvConfigId(config.getUuid())
            .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getAppId() + "-" + config.getServiceId() + "-"
                + config.getUuid())
            .timeSeriesDefinitions(Lists.newArrayList(DynaTraceTimeSeries.values()))
            .serviceMethods(DynatraceState.splitServiceMethods(config.getServiceMethods()))
            .startTime(startTime)
            .collectionTime(timeDuration)
            .dataCollectionMinute(0)
            .encryptedDataDetails(secretManager.getEncryptionDetails(dynaTraceConfig, config.getAppId(), null))
            .analysisComparisonStrategy(AnalysisComparisonStrategy.PREDICTIVE)
            .build();
    return createDelegateTask(
        config, waitId, new Object[] {dataCollectionInfo}, timeDuration, TaskType.DYNATRACE_COLLECT_24_7_METRIC_DATA);
  }

  private DelegateTask createAppDynamicsDelegateTask(
      AppDynamicsCVServiceConfiguration config, String waitId, long startTime, long endTime) {
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingsService.get(config.getConnectorId()).getValue();
    int timeDuration = (int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
    final AppdynamicsDataCollectionInfo dataCollectionInfo =
        AppdynamicsDataCollectionInfo.builder()
            .appDynamicsConfig(appDynamicsConfig)
            .applicationId(config.getAppId())
            .serviceId(config.getServiceId())
            .cvConfigId(config.getUuid())
            .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getAppId() + "-" + config.getServiceId() + "-"
                + config.getUuid())
            .startTime(startTime)
            .collectionTime(timeDuration)
            .appId(Long.parseLong(config.getAppDynamicsApplicationId()))
            .tierId(Long.parseLong(config.getTierId()))
            .dataCollectionMinute(0)
            .hosts(new HashMap<>())
            .encryptedDataDetails(secretManager.getEncryptionDetails(appDynamicsConfig, config.getAppId(), null))
            .timeSeriesMlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .build();
    return createDelegateTask(
        config, waitId, new Object[] {dataCollectionInfo}, timeDuration, TaskType.APPDYNAMICS_COLLECT_24_7_METRIC_DATA);
  }

  private DelegateTask createNewRelicDelegateTask(
      NewRelicCVServiceConfiguration config, String waitId, long startTime, long endTime) {
    final NewRelicConfig newRelicConfig = (NewRelicConfig) settingsService.get(config.getConnectorId()).getValue();
    int timeDuration = (int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
    Map<String, String> hostsMap = new HashMap<>();
    hostsMap.put("DUMMY_24_7_HOST", DEFAULT_GROUP_NAME);
    final NewRelicDataCollectionInfo dataCollectionInfo =
        NewRelicDataCollectionInfo.builder()
            .newRelicConfig(newRelicConfig)
            .applicationId(config.getAppId())
            .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getAppId() + "-" + config.getServiceId() + "-"
                + config.getUuid())
            .serviceId(config.getServiceId())
            .startTime(startTime)
            .cvConfigId(config.getUuid())
            .collectionTime(timeDuration)
            .newRelicAppId(Long.parseLong(config.getApplicationId()))
            .timeSeriesMlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .dataCollectionMinute(0)
            .hosts(hostsMap)
            .encryptedDataDetails(secretManager.getEncryptionDetails(newRelicConfig, config.getAppId(), null))
            .settingAttributeId(config.getConnectorId())
            .build();
    return createDelegateTask(
        config, waitId, new Object[] {dataCollectionInfo}, timeDuration, TaskType.NEWRELIC_COLLECT_24_7_METRIC_DATA);
  }

  private DelegateTask createPrometheusDelegateTask(
      PrometheusCVServiceConfiguration config, String waitId, long startTime, long endTime) {
    PrometheusConfig prometheusConfig = (PrometheusConfig) settingsService.get(config.getConnectorId()).getValue();
    int timeDuration = (int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
    final PrometheusDataCollectionInfo dataCollectionInfo =
        PrometheusDataCollectionInfo.builder()
            .prometheusConfig(prometheusConfig)
            .applicationId(config.getAppId())
            .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getAppId() + "-" + config.getServiceId() + "-"
                + config.getUuid())
            .serviceId(config.getServiceId())
            .cvConfigId(config.getUuid())
            .startTime(startTime)
            .collectionTime(timeDuration)
            .timeSeriesToCollect(config.getTimeSeriesToAnalyze())
            .hosts(new HashMap<>())
            .timeSeriesMlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .dataCollectionMinute(0)
            .build();
    return createDelegateTask(
        config, waitId, new Object[] {dataCollectionInfo}, timeDuration, TaskType.PROMETHEUS_COLLECT_24_7_METRIC_DATA);
  }

  private DelegateTask createDelegateTask(
      CVConfiguration request, String waitId, Object[] dataCollectionInfo, int timeDuration, TaskType taskType) {
    return aDelegateTask()
        .withTaskType(taskType)
        .withAccountId(request.getAccountId())
        .withAppId(request.getAppId())
        .withEnvId(request.getEnvId())
        .withWaitId(waitId)
        .withParameters(dataCollectionInfo)
        .withEnvId(request.getEnvId())
        .withTimeout(TimeUnit.MINUTES.toMillis(timeDuration + 120))
        .build();
  }

  private VerificationNodeDataSetupResponse getVerificationNodeDataResponse(
      String accountId, APMValidateCollectorConfig apmValidateCollectorConfig) {
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    String apmResponse =
        delegateProxyFactory.get(APMDelegateService.class, syncTaskContext).fetch(apmValidateCollectorConfig);
    JSONObject jsonObject = new JSONObject(apmResponse);
    boolean hasLoad = false;
    if (jsonObject.length() != 0) {
      hasLoad = true;
    }
    VerificationLoadResponse loadResponse =
        VerificationLoadResponse.builder().loadResponse(apmResponse).isLoadPresent(hasLoad).build();
    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(hasLoad)
        .loadResponse(loadResponse)
        .dataForNode(apmResponse)
        .build();
  }
}
