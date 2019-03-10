package software.wings.service.intfc.analysis;

import static software.wings.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.waiter.WaitNotifyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Base;
import software.wings.beans.BugsnagConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.bugsnag.BugsnagApplication;
import software.wings.service.impl.bugsnag.BugsnagDelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

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
                                          .appId(Base.GLOBAL_APP_ID)
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
}
