/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.utils;

import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class EmailHelperUtils {
  @Inject private SettingsService settingsService;

  public SmtpConfig getSmtpConfig(String accountId) {
    List<SettingAttribute> smtpAttribute =
        settingsService.getGlobalSettingAttributesByType(accountId, SettingVariableTypes.SMTP.name());
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
