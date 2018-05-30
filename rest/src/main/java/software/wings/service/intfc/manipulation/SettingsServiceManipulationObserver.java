package software.wings.service.intfc.manipulation;

import io.harness.observer.Rejection;
import software.wings.beans.SettingAttribute;

public interface SettingsServiceManipulationObserver {
  Rejection settingsServiceDeleting(SettingAttribute settingAttribute);
}
