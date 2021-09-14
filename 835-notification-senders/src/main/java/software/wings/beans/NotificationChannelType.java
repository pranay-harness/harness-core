/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.settings.SettingVariableTypes;

/**
 * Created by rishi on 10/30/16.
 */
@OwnedBy(PL)
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
