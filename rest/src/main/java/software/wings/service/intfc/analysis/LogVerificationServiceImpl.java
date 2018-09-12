package software.wings.service.intfc.analysis;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import software.wings.annotation.Encryptable;
import software.wings.beans.Base;
import software.wings.beans.BugsnagConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.bugsnag.BugsnagApplication;
import software.wings.service.impl.bugsnag.BugsnagDelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

public class LogVerificationServiceImpl implements LogVerificationService {
  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;

  @Override
  public Set<BugsnagApplication> getOrgProjectListBugsnag(
      String settingId, String orgId, StateType stateType, boolean shouldGetProjects) {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();

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
}
