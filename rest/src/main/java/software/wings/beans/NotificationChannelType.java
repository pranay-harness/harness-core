package software.wings.beans;

import software.wings.settings.SettingValue.SettingVariableTypes;

/**
 * Created by rishi on 10/30/16.
 */
public enum NotificationChannelType {
  EMAIL("Email", SettingVariableTypes.SMTP),
  SLACK("Slack", SettingVariableTypes.SLACK),
  WEB("WEB", null);

  private final String displayName;
  private final SettingVariableTypes settingVariableTypes;

  NotificationChannelType(String displayName, SettingVariableTypes settingVariableTypes) {
    this.displayName = displayName;
    this.settingVariableTypes = settingVariableTypes;
  }

  public String getDisplayName() {
    return displayName;
  }

  public SettingVariableTypes getSettingVariableTypes() {
    return settingVariableTypes;
  }
}
