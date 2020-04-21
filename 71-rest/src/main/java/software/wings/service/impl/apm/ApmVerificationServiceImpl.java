package software.wings.service.impl.apm;

import static software.wings.settings.SettingValue.SettingVariableTypes.APM_VERIFICATION;

import com.google.inject.Inject;

import software.wings.beans.APMVerificationConfig;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.service.intfc.apm.ApmVerificationService;

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
