/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.manipulation;

import io.harness.observer.Rejection;

import software.wings.beans.SettingAttribute;

public interface SettingsServiceManipulationObserver {
  Rejection settingsServiceDeleting(SettingAttribute settingAttribute);
}
