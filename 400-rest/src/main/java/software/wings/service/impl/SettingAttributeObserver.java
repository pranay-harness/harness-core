/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.SettingAttribute;

@OwnedBy(HarnessTeam.CDC)
public interface SettingAttributeObserver {
  void onSaved(SettingAttribute settingAttribute);
  void onUpdated(SettingAttribute prevSettingAttribute, SettingAttribute currSettingAttribute);
  void onDeleted(SettingAttribute settingAttribute);
}
