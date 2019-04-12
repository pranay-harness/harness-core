package software.wings.service.intfc.analysis;

import static io.harness.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.waiter.WaitNotifyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.BugsnagConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.impl.bugsnag.BugsnagApplication;
import software.wings.service.impl.bugsnag.BugsnagDelegateService;
import software.wings.service.impl.bugsnag.BugsnagSetupTestData;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
public class LogVerificationServiceImpl implements LogVerificationService {
  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  private static final Logger logger = LoggerFactory.getLogger(LogVerificationServiceImpl.class);

  @Override
  public Set<BugsnagApplication> getOrgProjectListBugsnag(
      String settingId, String orgId, StateType stateType, boolean shouldGetProjects) {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(settingAttribute.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();

    switch (stateType) {
      case BUG_SNAG:
        BugsnagConfig config = (BugsnagConfig) settingAttribute.getValue();
        if (shouldGetProjects) {
          return delegateProxyFactory.get(BugsnagDelegateService.class, syncTaskContext)
              .getProjects(config, orgId, encryptionDetails, null);
        } else {
          return delegateProxyFactory.get(BugsnagDelegateService.class, syncTaskContext)
              .getOrganizations((BugsnagConfig) settingAttribute.getValue(), encryptionDetails, null);
        }

      default:
        throw new WingsException("Unknown state type in getOrgProjectListBugsnag");
    }
  }

  @Override
  public boolean sendNotifyForLogAnalysis(String correlationId, LogAnalysisResponse response) {
    try {
      waitNotifyEngine.notify(correlationId, response);
      return true;
    } catch (Exception ex) {
      logger.error("Exception while notifying correlationId {}", correlationId, ex);
      return false;
    }
  }

  @Override
  public VerificationNodeDataSetupResponse getTestLogData(String accountId, BugsnagSetupTestData bugsnagSetupTestData) {
    logger.info(
        "Starting Log Data collection for account Id : {}, BugsnagSetupTestData : {}", accountId, bugsnagSetupTestData);
    // gets the settings attributes for given settings id
    final SettingAttribute settingAttribute = settingsService.get(bugsnagSetupTestData.getSettingId());
    logger.info("Settings attribute : " + settingAttribute);
    if (settingAttribute == null) {
      throw new WingsException(
          "No " + StateType.BUG_SNAG + " setting with id: " + bugsnagSetupTestData.getSettingId() + " found");
    }

    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
    SyncTaskContext taskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    bugsnagSetupTestData.setToTime(System.currentTimeMillis());
    bugsnagSetupTestData.setFromTime(bugsnagSetupTestData.getToTime() - TimeUnit.MINUTES.toMillis(60));
    Object response;
    try {
      response = delegateProxyFactory.get(BugsnagDelegateService.class, taskContext)
                     .search((BugsnagConfig) settingAttribute.getValue(), accountId, bugsnagSetupTestData,
                         encryptedDataDetails,
                         createApiCallLog(settingAttribute.getAccountId(), bugsnagSetupTestData.getAppId(),
                             bugsnagSetupTestData.getGuid()));
    } catch (IOException ex) {
      logger.info("Error while getting data ", ex);
      return VerificationNodeDataSetupResponse.builder().providerReachable(false).build();
    }
    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(true)
        .loadResponse(VerificationLoadResponse.builder()
                          .isLoadPresent(!((ArrayList) response).isEmpty())
                          .loadResponse(response)
                          .build())
        .build();
  }
}
