package software.wings.service.impl.appdynamics;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.impl.ThirdPartyApiCallLog.NO_STATE_EXECUTION_ID;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
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
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

  @Override
  public List<NewRelicApplication> getApplications(final String settingId) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(settingAttribute.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(appDynamicsConfig, null, null);
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
        .getAllApplications(appDynamicsConfig, encryptionDetails);
  }

  @Override
  public Set<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId) throws IOException {
    return this.getTiers(
        settingId, appdynamicsAppId, ThirdPartyApiCallLog.createApiCallLog(GLOBAL_ACCOUNT_ID, NO_STATE_EXECUTION_ID));
  }

  @Override
  public Set<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId, ThirdPartyApiCallLog apiCallLog)
      throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(settingAttribute.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(appDynamicsConfig, null, null);
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
        .getTiers(appDynamicsConfig, appdynamicsAppId, encryptionDetails, apiCallLog);
  }

  @Override
  public Set<AppdynamicsTier> getDependentTiers(String settingId, long appdynamicsAppId, AppdynamicsTier tier)
      throws IOException {
    return getDependentTiers(settingId, appdynamicsAppId, tier,
        ThirdPartyApiCallLog.createApiCallLog(GLOBAL_ACCOUNT_ID, NO_STATE_EXECUTION_ID));
  }
  @Override
  public Set<AppdynamicsTier> getDependentTiers(String settingId, long appdynamicsAppId, AppdynamicsTier tier,
      ThirdPartyApiCallLog apiCallLog) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(settingAttribute.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(appDynamicsConfig, null, null);

    Set<AppdynamicsTier> tierDependencies =
        delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
            .getTierDependencies(appDynamicsConfig, appdynamicsAppId, encryptionDetails, apiCallLog);

    return getDependentTiers(tierDependencies, tier);
  }

  private Set<AppdynamicsTier> getDependentTiers(Set<AppdynamicsTier> tierMap, AppdynamicsTier analyzedTier) {
    Set<AppdynamicsTier> dependentTiers = new HashSet<>();
    for (AppdynamicsTier tier : tierMap) {
      String dependencyPath = getDependencyPath(tier, analyzedTier);
      if (!isEmpty(dependencyPath)) {
        tier.setDependencyPath(dependencyPath);
        dependentTiers.add(tier);
      }
    }
    return dependentTiers;
  }

  private String getDependencyPath(AppdynamicsTier tier, AppdynamicsTier analyzedTier) {
    if (isEmpty(tier.getExternalTiers())) {
      return null;
    }

    if (tier.getExternalTiers().contains(analyzedTier)) {
      return tier.getName() + "->" + analyzedTier.getName();
    }

    for (AppdynamicsTier externalTier : tier.getExternalTiers()) {
      String dependencyPath = getDependencyPath(externalTier, analyzedTier);
      if (dependencyPath != null) {
        return tier.getName() + "->" + dependencyPath;
      }
    }

    return null;
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
    try {
      List<NewRelicApplication> apps = getApplications(connectorId);
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
    return getTier(connectorId, appdynamicsAppId, tierId,
        ThirdPartyApiCallLog.createApiCallLog(GLOBAL_ACCOUNT_ID, NO_STATE_EXECUTION_ID));
  }

  @Override
  public AppdynamicsTier getTier(
      String connectorId, long appdynamicsAppId, String tierId, ThirdPartyApiCallLog apiCallLog) {
    try {
      AppdynamicsTier appdynamicsTier = null;
      Set<AppdynamicsTier> tiers = getTiers(connectorId, appdynamicsAppId, apiCallLog);
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
  public String getAppDynamicsApplicationByName(String analysisServerConfigId, String applicationName) {
    try {
      String applicationId = null;
      List<NewRelicApplication> apps = getApplications(analysisServerConfigId);
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
  public String getTierByName(
      String analysisServerConfigId, String applicationId, String tierName, ThirdPartyApiCallLog apiCallLog) {
    try {
      String tierId = null;
      Set<AppdynamicsTier> tiers = getTiers(analysisServerConfigId, Long.parseLong(applicationId), apiCallLog);
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
}
