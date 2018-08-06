package software.wings.service.impl.newrelic;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.service.impl.ThirdPartyApiCallLog.apiCallLogWithDummyStateExecution;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.mongodb.morphia.query.Sort;
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
import software.wings.beans.WorkflowExecution;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.APMDelegateService;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.newrelic.NewRelicApplication.NewRelicApplications;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextFactory;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.utils.CacheHelper;
import software.wings.utils.Misc;

import java.util.List;
import java.util.concurrent.TimeUnit;
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
  @Override
  public void validateAPMConfig(SettingAttribute settingAttribute, APMValidateCollectorConfig config) {
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      delegateProxyFactory.get(APMDelegateService.class, syncTaskContext).validateCollector(config);
    } catch (Exception e) {
      String errorMsg = e.getCause() != null ? Misc.getMessage(e.getCause()) : Misc.getMessage(e);
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR).addParam("reason", errorMsg);
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
              .encryptedDataDetails(apmVerificationConfig.encryptedDataDetails(secretManager))
              .build();
      SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
      return delegateProxyFactory.get(APMDelegateService.class, syncTaskContext).fetch(apmValidateCollectorConfig);
    } catch (Exception e) {
      String errorMsg = e.getCause() != null ? Misc.getMessage(e.getCause()) : Misc.getMessage(e);
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR).addParam("reason", errorMsg);
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
      throw new WingsException(errorCode).addParam("reason", Misc.getMessage(e));
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
      throw new WingsException(errorCode).addParam(
          "message", "Error in getting new relic applications. " + Misc.getMessage(e));
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
      throw new WingsException(errorCode).addParam(
          "message", "Error in getting new relic applications. " + e.getMessage());
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
      throw new WingsException(ErrorCode.NEWRELIC_ERROR)
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
      throw new WingsException(ErrorCode.NEWRELIC_ERROR)
          .addParam("message", "Error in getting metric data for the node. " + e.getMessage());
    }
  }

  @Override
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      NewRelicSetupTestNodeData setupTestNodeData) {
    WorkflowExecution workflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                              .filter("appId", setupTestNodeData.getAppId())
                                              .filter("workflowId", setupTestNodeData.getWorkflowId())
                                              .filter("status", SUCCESS)
                                              .order(Sort.descending("createdAt"))
                                              .get();

    if (workflowExecution == null) {
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR)
          .addParam("reason", "No successful execution exists for the workflow.");
    }

    StateExecutionInstance stateExecutionInstance = wingsPersistence.createQuery(StateExecutionInstance.class)
                                                        .filter("executionUuid", workflowExecution.getUuid())
                                                        .filter("stateType", StateType.PHASE)
                                                        .order(Sort.descending("createdAt"))
                                                        .get();
    ExecutionContext executionContext = executionContextFactory.createExecutionContext(stateExecutionInstance, null);
    String hostName = isEmpty(setupTestNodeData.getHostExpression())
        ? setupTestNodeData.getInstanceName()
        : executionContext.renderExpression(
              setupTestNodeData.getHostExpression(), Lists.newArrayList(setupTestNodeData.getInstanceElement()));
    logger.info("rendered host is {}", hostName);

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
      throw new WingsException(ErrorCode.NEWRELIC_CONFIGURATION_ERROR)
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
}
