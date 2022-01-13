/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.apm;

import static software.wings.settings.SettingVariableTypes.APM_VERIFICATION;

import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedDataParent;

import software.wings.beans.APMVerificationConfig;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.apm.ApmVerificationService;

import com.google.inject.Inject;
import java.util.Map;

public class ApmVerificationServiceImpl implements ApmVerificationService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void addParents(SettingAttribute settingAttribute) {
    APMVerificationConfig apmVerificationConfig = (APMVerificationConfig) settingAttribute.getValue();
    apmVerificationConfig.getSecretIdsToFieldNameMap().forEach((secretId, secretName) -> {
      final EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, secretId);
      encryptedData.addParent(new EncryptedDataParent(settingAttribute.getUuid(), APM_VERIFICATION, secretName));
      wingsPersistence.save(encryptedData);
    });
  }

  @Override
  public void updateParents(SettingAttribute savedSettingAttribute, Map<String, String> existingSecretRefs) {
    APMVerificationConfig apmVerificationConfig = (APMVerificationConfig) savedSettingAttribute.getValue();
    final Map<String, String> updatedSecretRefs = apmVerificationConfig.getSecretIdsToFieldNameMap();

    addParents(savedSettingAttribute);
    updatedSecretRefs.forEach((secretId, secretName) -> existingSecretRefs.remove(secretId));
    existingSecretRefs.forEach((secretId, secretName) -> {
      final EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, secretId);
      encryptedData.removeParent(
          new EncryptedDataParent(savedSettingAttribute.getUuid(), APM_VERIFICATION, secretName));
      wingsPersistence.save(encryptedData);
    });
  }
}
