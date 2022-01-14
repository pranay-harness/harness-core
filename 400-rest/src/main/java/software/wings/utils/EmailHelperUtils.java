package software.wings.utils;

import io.harness.ng.core.account.DefaultExperience;

import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class EmailHelperUtils {
  @Inject private SettingsService settingsService;
  @Inject private AccountService accountService;
  static final String NG_SMTP_SETTINGS_PREFIX = "ngSmtpConfig-";

  public SmtpConfig getSmtpConfig(String accountId) {
    if (accountId == null) {
      return null;
    }
    List<SettingAttribute> smtpAttribute =
        settingsService.getGlobalSettingAttributesByType(accountId, SettingVariableTypes.SMTP.name());
    Account account = accountService.get(accountId);
    if (DefaultExperience.NG.equals(account.getDefaultExperience())) {
      for (SettingAttribute settingAttribute : smtpAttribute) {
        String smtpAttributeName = settingAttribute.getName();
        if (smtpAttributeName.length() >= 13
            && (NG_SMTP_SETTINGS_PREFIX.equalsIgnoreCase(settingAttribute.getName().substring(0, 13)))) {
          SmtpConfig config = (SmtpConfig) settingAttribute.getValue();
          if (isSmtpConfigValid(config)) {
            return config;
          }
        }
      }
    }
    if (smtpAttribute.size() > 0) {
      for (SettingAttribute settingAttribute : smtpAttribute) {
        SmtpConfig config = (SmtpConfig) settingAttribute.getValue();
        if (isSmtpConfigValid(config)) {
          return config;
        }
      }
    }
    return null;
  }

  public boolean isSmtpConfigValid(SmtpConfig config) {
    return config != null && config.valid();
  }
}
