package software.wings.service.impl.appdynamics;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.impl.ThirdPartyApiCallLog.NO_STATE_EXECUTION_ID;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.appd.AppDynamicsApplication;
import io.harness.cvng.beans.appd.AppDynamicsTier;
import io.harness.cvng.beans.appd.AppdynamicsMetricPackDataValidationRequest;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.apm.MLServiceUtils;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.service.intfc.security.NGSecretService;
import software.wings.service.intfc.security.SecretManager;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rsingh on 4/17/17.
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class AppdynamicsServiceImpl implements AppdynamicsService {
  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private MLServiceUtils mlServiceUtils;
  @Inject private NGSecretService ngSecretService;
  @Override
  public List<NewRelicApplication> getApplications(final String settingId) {
    return this.getApplications(settingId, null, null);
  }

  @Override
  public List<NewRelicApplication> getApplications(String settingId, String appId, String workflowExecutionId) {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(settingAttribute.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(appDynamicsConfig, appId, workflowExecutionId);
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
        .getAllApplications(appDynamicsConfig, encryptionDetails);
  }

  @Override
  public List<AppDynamicsApplication> getApplications(AppDynamicsConnectorDTO appDynamicsConnector) {
    NGAccess basicNGAccessObject =
        BaseNGAccess.builder().accountIdentifier(appDynamicsConnector.getAccountId()).build();
    List<EncryptedDataDetail> encryptedDataDetails =
        ngSecretService.getEncryptionDetails(basicNGAccessObject, appDynamicsConnector);

    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(appDynamicsConnector.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
        .getApplications(appDynamicsConnector, encryptedDataDetails);
  }

  @Override
  public Set<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId) {
    return this.getTiers(
        settingId, appdynamicsAppId, ThirdPartyApiCallLog.createApiCallLog(GLOBAL_ACCOUNT_ID, NO_STATE_EXECUTION_ID));
  }

  @Override
  public Set<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId, ThirdPartyApiCallLog apiCallLog) {
    return this.getTiers(settingId, appdynamicsAppId, null, null, apiCallLog);
  }

  @Override
  public Set<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId, String appId,
      String workflowExecutionId, ThirdPartyApiCallLog apiCallLog) {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(settingAttribute.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(appDynamicsConfig, appId, workflowExecutionId);
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
        .getTiers(appDynamicsConfig, appdynamicsAppId, encryptionDetails, apiCallLog);
  }

  @Override
  public Set<AppDynamicsTier> getTiers(long appDynamicsAppId, AppDynamicsConnectorDTO appDynamicsConnector) {
    NGAccess basicNGAccessObject =
        BaseNGAccess.builder().accountIdentifier(appDynamicsConnector.getAccountId()).build();
    List<EncryptedDataDetail> encryptedDataDetails =
        ngSecretService.getEncryptionDetails(basicNGAccessObject, appDynamicsConnector);

    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(appDynamicsConnector.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
        .getTiers(appDynamicsConnector, encryptedDataDetails, appDynamicsAppId);
  }

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(AppdynamicsSetupTestNodeData setupTestNodeData) {
    String hostName = null;
    // check if it is for service level, serviceId is empty then get hostname
    if (!setupTestNodeData.isServiceLevel()) {
      hostName = mlServiceUtils.getHostName(setupTestNodeData);
    }

    try {
      final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(settingAttribute.getAccountId())
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                            .build();
      return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
          .getMetricsWithDataForNode((AppDynamicsConfig) settingAttribute.getValue(), encryptionDetails,
              setupTestNodeData, hostName,
              createApiCallLog(settingAttribute.getAccountId(), setupTestNodeData.getGuid()));
    } catch (Exception e) {
      logger.info("error getting metric data for node", e);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR, e).addParam("reason", e.getMessage());
    }
  }

  @Override
  public NewRelicApplication getAppDynamicsApplication(String connectorId, String appDynamicsApplicationId) {
    return this.getAppDynamicsApplication(connectorId, appDynamicsApplicationId, null, null);
  }

  @Override
  public NewRelicApplication getAppDynamicsApplication(
      String connectorId, String appDynamicsApplicationId, String appId, String workflowExecutionId) {
    try {
      List<NewRelicApplication> apps = getApplications(connectorId, appId, workflowExecutionId);
      NewRelicApplication appDynamicsApp = null;
      for (NewRelicApplication app : apps) {
        if (String.valueOf(app.getId()).equals(appDynamicsApplicationId)) {
          appDynamicsApp = app;
          break;
        }
      }
      return appDynamicsApp;
    } catch (Exception ex) {
      throw new WingsException(ex.getMessage());
    }
  }

  @Override
  public AppdynamicsTier getTier(String connectorId, long appdynamicsAppId, String tierId) {
    return getTier(connectorId, appdynamicsAppId, tierId, null, null,
        ThirdPartyApiCallLog.createApiCallLog(GLOBAL_ACCOUNT_ID, NO_STATE_EXECUTION_ID));
  }

  @Override
  public AppdynamicsTier getTier(String connectorId, long appdynamicsAppId, String tierId, String appId,
      String workflowExecutionId, ThirdPartyApiCallLog apiCallLog) {
    try {
      AppdynamicsTier appdynamicsTier = null;
      Set<AppdynamicsTier> tiers = getTiers(connectorId, appdynamicsAppId, appId, workflowExecutionId, apiCallLog);
      for (AppdynamicsTier tier : tiers) {
        if (String.valueOf(tier.getId()).equals(tierId)) {
          appdynamicsTier = tier;
          break;
        }
      }
      return appdynamicsTier;
    } catch (Exception ex) {
      throw new WingsException(ex.getMessage());
    }
  }

  @Override
  public String getAppDynamicsApplicationByName(
      String analysisServerConfigId, String applicationName, String appId, String workflowExecutionId) {
    try {
      String applicationId = null;
      List<NewRelicApplication> apps = getApplications(analysisServerConfigId, appId, workflowExecutionId);
      for (NewRelicApplication app : apps) {
        if (String.valueOf(app.getName()).equals(applicationName)) {
          applicationId = String.valueOf(app.getId());
          break;
        }
      }
      if (isEmpty(applicationId)) {
        throw new WingsException("Invalid AppDynamics Application Name provided : " + applicationName);
      }
      return applicationId;
    } catch (Exception ex) {
      throw new WingsException(ex.getMessage());
    }
  }

  @Override
  public String getTierByName(String analysisServerConfigId, String applicationId, String tierName, String appId,
      String workflowExecutionId, ThirdPartyApiCallLog apiCallLog) {
    try {
      String tierId = null;
      Set<AppdynamicsTier> tiers =
          getTiers(analysisServerConfigId, Long.parseLong(applicationId), appId, workflowExecutionId, apiCallLog);
      for (AppdynamicsTier tier : tiers) {
        if (String.valueOf(tier.getName()).equals(tierName)) {
          tierId = String.valueOf(tier.getId());
          break;
        }
      }
      if (isEmpty(tierId)) {
        throw new WingsException("Invalid AppDynamics Tier Name provided : " + tierName);
      }
      return tierId;
    } catch (Exception ex) {
      throw new WingsException(ex.getMessage());
    }
  }

  @Override
  public Set<AppdynamicsValidationResponse> getMetricPackData(String accountId, String projectIdentifier,
      long appdAppId, long appdTierId, String requestGuid,
      AppdynamicsMetricPackDataValidationRequest validationRequest) {
    logger.info("for {} getting data for {}", projectIdentifier, validationRequest);
    Preconditions.checkState(isNotEmpty(validationRequest.getMetricPacks()),
        "No metric packs found for project {} with the name {}", projectIdentifier, validationRequest.getMetricPacks());
    NGAccess basicNGAccessObject =
        BaseNGAccess.builder().accountIdentifier(validationRequest.getConnector().getAccountId()).build();
    List<EncryptedDataDetail> encryptedDataDetails =
        ngSecretService.getEncryptionDetails(basicNGAccessObject, validationRequest.getConnector());
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(validationRequest.getConnector().getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
        .getMetricPackData(validationRequest.getConnector(), encryptedDataDetails, appdAppId, appdTierId, requestGuid,
            validationRequest.getMetricPacks(), Instant.now().minusSeconds(TimeUnit.HOURS.toSeconds(1)), Instant.now());
  }
}
