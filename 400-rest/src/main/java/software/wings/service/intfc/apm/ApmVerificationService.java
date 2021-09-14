/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.apm;

import software.wings.beans.SettingAttribute;

import java.util.Map;

public interface ApmVerificationService {
  void addParents(SettingAttribute settingAttribute);
  void updateParents(SettingAttribute savedSettingAttribute, Map<String, String> existingSecretRefsForApm);
}
