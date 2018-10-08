package software.wings.service.impl.appdynamics;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.service.impl.ThirdPartyApiCallLog.apiCallLogWithDummyStateExecution;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.SettingAttribute;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.apm.MLServiceUtil;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rsingh on 4/17/17.
 */
@ValidateOnExecution
public class AppdynamicsServiceImpl implements AppdynamicsService {
  private static final Logger logger = LoggerFactory.getLogger(AppdynamicsServiceImpl.class);

  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private MLServiceUtil mlServiceUtil;

  @Override
  public List<NewRelicApplication> getApplications(final String settingId) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(appDynamicsConfig, null, null);
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
        .getAllApplications(appDynamicsConfig, encryptionDetails);
  }

  @Override
  public Set<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(appDynamicsConfig, null, null);
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
        .getTiers(appDynamicsConfig, appdynamicsAppId, encryptionDetails);
  }

  @Override
  public Set<AppdynamicsTier> getDependentTiers(String settingId, long appdynamicsAppId, AppdynamicsTier tier)
      throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(appDynamicsConfig, null, null);

    Set<AppdynamicsTier> tierDependencies =
        delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
            .getTierDependencies(appDynamicsConfig, appdynamicsAppId, encryptionDetails);

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
  public boolean validateConfig(final SettingAttribute settingAttribute) {
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
      return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
          .validateConfig(appDynamicsConfig);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.APPDYNAMICS_CONFIGURATION_ERROR).addParam("reason", Misc.getMessage(e));
    }
  }

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(AppdynamicsSetupTestNodeData setupTestNodeData) {
    String hostName = mlServiceUtil.getHostNameFromExpression(setupTestNodeData);
    try {
      final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = aContext()
                                            .withAccountId(settingAttribute.getAccountId())
                                            .withAppId(Base.GLOBAL_APP_ID)
                                            .withTimeout(Constants.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                            .build();
      return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
          .getMetricsWithDataForNode((AppDynamicsConfig) settingAttribute.getValue(), encryptionDetails,
              setupTestNodeData.getApplicationId(), setupTestNodeData.getTierId(), hostName,
              setupTestNodeData.getFromTime(), setupTestNodeData.getToTime(),
              apiCallLogWithDummyStateExecution(settingAttribute.getAccountId()));
    } catch (Exception e) {
      logger.info("error getting metric data for node", e);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
          .addParam("message", "Error in getting metric data for the node. " + e.getMessage());
    }
  }
}
