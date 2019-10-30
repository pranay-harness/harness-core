package software.wings.service.impl;

import software.wings.beans.SettingAttribute;

public interface SettingAttributeObserver {
  void onUpdated(SettingAttribute prevSettingAttribute, SettingAttribute currSettingAttribute);
  void onDeleted(SettingAttribute settingAttribute);
}
