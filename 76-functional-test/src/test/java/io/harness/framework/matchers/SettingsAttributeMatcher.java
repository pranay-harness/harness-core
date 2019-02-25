package io.harness.framework.matchers;

import software.wings.beans.SettingAttribute;

public class SettingsAttributeMatcher<T> implements Matcher {
  @Override
  public boolean matches(Object expected, Object actual) {
    if (actual == null) {
      return false;
    }
    SettingAttribute settingAttribute = (SettingAttribute) actual;
    if (settingAttribute != null) {
      return true;
    }
    return false;
  }
}
