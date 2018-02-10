package software.wings.service.impl.appdynamics;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rsingh on 4/17/17.
 */
@ValidateOnExecution
public class AppdynamicsServiceImpl implements AppdynamicsService {
  private static final Logger logger = LoggerFactory.getLogger(AppdynamicsDelegateServiceImpl.class);
  private static final long APPDYNAMICS_CALL_TIMEOUT = TimeUnit.MINUTES.toMillis(1L);

  @Inject private SettingsService settingsService;

  @Inject private DelegateProxyFactory delegateProxyFactory;

  @Inject private SecretManager secretManager;

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
  public List<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(appDynamicsConfig, null, null);
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
        .getTiers(appDynamicsConfig, appdynamicsAppId, encryptionDetails);
  }

  @Override
  public List<AppdynamicsBusinessTransaction> getBusinessTransactions(String settingId, long appdynamicsAppId)
      throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();

    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(appDynamicsConfig, null, null);
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
        .getBusinessTransactions(appDynamicsConfig, appdynamicsAppId, encryptionDetails);
  }

  @Override
  public List<AppdynamicsMetric> getTierBTMetrics(String settingId, long appdynamicsAppId, long tierId)
      throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    syncTaskContext.setTimeout(APPDYNAMICS_CALL_TIMEOUT);
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(appDynamicsConfig, null, null);
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
        .getTierBTMetrics(appDynamicsConfig, appdynamicsAppId, tierId, encryptionDetails);
  }

  @Override
  public List<AppdynamicsMetricData> getTierBTMetricData(
      String settingId, long appdynamicsAppId, long tierId, String btName, int durantionInMinutes) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    syncTaskContext.setTimeout(APPDYNAMICS_CALL_TIMEOUT);
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(appDynamicsConfig, null, null);
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
        .getTierBTMetricData(
            appDynamicsConfig, appdynamicsAppId, tierId, btName, durantionInMinutes, encryptionDetails);
  }

  @Override
  public void validateConfig(final SettingAttribute settingAttribute) {
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
      delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext).validateConfig(appDynamicsConfig);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.APPDYNAMICS_CONFIGURATION_ERROR).addParam("reason", e.getMessage());
    }
  }
}
