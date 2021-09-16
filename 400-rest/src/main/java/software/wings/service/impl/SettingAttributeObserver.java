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
