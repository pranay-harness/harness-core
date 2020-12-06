package software.wings.service.intfc.apm;

import software.wings.beans.SettingAttribute;

import java.util.Map;

public interface ApmVerificationService {
  void addParents(SettingAttribute settingAttribute);
  void updateParents(SettingAttribute savedSettingAttribute, Map<String, String> existingSecretRefsForApm);
}
