package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.APMFetchConfig;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.analysis.APMVerificationService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
import software.wings.verification.CVConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.concurrent.TimeUnit;

/**
 * @author Praveen 9/6/18
 */

public class APMVerificationServiceImpl implements APMVerificationService {
  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;
  @Inject private WingsPersistence wingsPersistence;

  private static final Logger logger = LoggerFactory.getLogger(APMVerificationServiceImpl.class);

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(
      String accountId, String serverConfigId, APMFetchConfig fetchConfig) {
    try {
      if (isEmpty(serverConfigId) || fetchConfig == null) {
        throw new WingsException("Invalid Parameters passed while trying to get test data for APM");
      }
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
      String apmResponse =
          delegateProxyFactory.get(APMDelegateService.class, syncTaskContext).fetch(apmValidateCollectorConfig);
      // if response is an empty JSON, we will tag it as no-load.
      JSONObject jsonObject = new JSONObject(apmResponse);
      boolean hasLoad = false;
      if (jsonObject.length() != 0) {
        hasLoad = true;
      }
      VerificationLoadResponse loadResponse =
          VerificationLoadResponse.builder().loadResponse(apmResponse).isLoadPresent(hasLoad).build();
      return VerificationNodeDataSetupResponse.builder().loadResponse(loadResponse).dataForNode(apmResponse).build();

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
  public boolean collect247Data(
      String cvConfigId, StateType stateType, long startTime, long endTime, int lastDataCollectionMinute) {
    switch (stateType) {
      case APP_DYNAMICS:
        AppDynamicsCVServiceConfiguration config =
            (AppDynamicsCVServiceConfiguration) wingsPersistence.createQuery(CVConfiguration.class)
                .filter("_id", cvConfigId)
                .get();
        String waitId = generateUuid();
        DelegateTask task = createAppDynamicsDelegateTask(config, waitId, startTime, endTime, lastDataCollectionMinute);
        waitNotifyEngine.waitForAll(
            new DataCollectionCallback(null, null, false), waitId); // TODO: is passing nulls here, okay?
        logger.info("Queuing 24x7 data collection task for AppDynamics, cvConfigurationId: {}", cvConfigId);
        delegateService.queueTask(task);
        return true;
      default:
        logger.error("Calling collect 24x7 data for a non-appDynamics state");
        return false;
    }
  }

  private DelegateTask createAppDynamicsDelegateTask(AppDynamicsCVServiceConfiguration request, String waitId,
      long startTime, long endTime, int lastDataCollectionMinute) {
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingsService.get(request.getConnectorId()).getValue();
    int timeDuration = (int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
    final AppdynamicsDataCollectionInfo dataCollectionInfo =
        AppdynamicsDataCollectionInfo.builder()
            .appDynamicsConfig(appDynamicsConfig)
            .applicationId(request.getAppId())
            .serviceId(request.getServiceId())
            .cvConfigId(request.getUuid())
            .stateExecutionId(CV_24x7_STATE_EXECUTION)
            .startTime(startTime)
            .collectionTime(timeDuration)
            .appId(Long.parseLong(request.getAppDynamicsApplicationId()))
            .tierId(Long.parseLong(request.getTierId()))
            .dataCollectionMinute(lastDataCollectionMinute)
            .encryptedDataDetails(secretManager.getEncryptionDetails(appDynamicsConfig, request.getAppId(), null))
            .timeSeriesMlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .build();

    return aDelegateTask()
        .withTaskType(TaskType.APPDYNAMICS_COLLECT_24_7_METRIC_DATA)
        .withAccountId(request.getAccountId())
        .withAppId(request.getAppId())
        .withEnvId(request.getEnvId())
        .withWaitId(waitId)
        .withParameters(new Object[] {dataCollectionInfo})
        .withEnvId(request.getEnvId())
        .withTimeout(TimeUnit.MINUTES.toMillis(timeDuration + 120))
        .build();
  }
}
