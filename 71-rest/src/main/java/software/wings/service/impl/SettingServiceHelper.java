package software.wings.service.impl;

import static io.harness.encryption.EncryptionReflectUtils.getEncryptedRefField;
import static software.wings.beans.SettingAttribute.SettingCategory.AZURE_ARTIFACTS;
import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;
import static software.wings.beans.SettingAttribute.SettingCategory.HELM_REPO;
import static software.wings.settings.SettingValue.SettingVariableTypes.AMAZON_S3_HELM_REPO;
import static software.wings.settings.SettingValue.SettingVariableTypes.APP_DYNAMICS;
import static software.wings.settings.SettingValue.SettingVariableTypes.ARTIFACTORY;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.AZURE;
import static software.wings.settings.SettingValue.SettingVariableTypes.AZURE_ARTIFACTS_PAT;
import static software.wings.settings.SettingValue.SettingVariableTypes.BAMBOO;
import static software.wings.settings.SettingValue.SettingVariableTypes.BUG_SNAG;
import static software.wings.settings.SettingValue.SettingVariableTypes.CLOUD_WATCH;
import static software.wings.settings.SettingValue.SettingVariableTypes.DATA_DOG;
import static software.wings.settings.SettingValue.SettingVariableTypes.DATA_DOG_LOG;
import static software.wings.settings.SettingValue.SettingVariableTypes.DOCKER;
import static software.wings.settings.SettingValue.SettingVariableTypes.DYNA_TRACE;
import static software.wings.settings.SettingValue.SettingVariableTypes.ELK;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCS_HELM_REPO;
import static software.wings.settings.SettingValue.SettingVariableTypes.GIT;
import static software.wings.settings.SettingValue.SettingVariableTypes.HTTP_HELM_REPO;
import static software.wings.settings.SettingValue.SettingVariableTypes.INSTANA;
import static software.wings.settings.SettingValue.SettingVariableTypes.JENKINS;
import static software.wings.settings.SettingValue.SettingVariableTypes.JIRA;
import static software.wings.settings.SettingValue.SettingVariableTypes.KUBERNETES_CLUSTER;
import static software.wings.settings.SettingValue.SettingVariableTypes.LOGZ;
import static software.wings.settings.SettingValue.SettingVariableTypes.NEW_RELIC;
import static software.wings.settings.SettingValue.SettingVariableTypes.NEXUS;
import static software.wings.settings.SettingValue.SettingVariableTypes.PCF;
import static software.wings.settings.SettingValue.SettingVariableTypes.PROMETHEUS;
import static software.wings.settings.SettingValue.SettingVariableTypes.SERVICENOW;
import static software.wings.settings.SettingValue.SettingVariableTypes.SFTP;
import static software.wings.settings.SettingValue.SettingVariableTypes.SMB;
import static software.wings.settings.SettingValue.SettingVariableTypes.SMTP;
import static software.wings.settings.SettingValue.SettingVariableTypes.SPLUNK;
import static software.wings.settings.SettingValue.SettingVariableTypes.SPOT_INST;
import static software.wings.settings.SettingValue.SettingVariableTypes.SUMO;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.Encryptable;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.EncryptionReflectUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.FeatureName;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Singleton
public class SettingServiceHelper {
  private static final String REFERENCED_SECRET_ERROR_MSG = "Unable to copy encryption details";
  public static final Set<SettingVariableTypes> ATTRIBUTES_USING_REFERENCES = Sets.immutableEnumSet(AWS, AZURE, GCP,
      KUBERNETES_CLUSTER, PCF, SPOT_INST, APP_DYNAMICS, NEW_RELIC, INSTANA, PROMETHEUS, DATA_DOG, DYNA_TRACE,
      CLOUD_WATCH, DATA_DOG_LOG, BUG_SNAG, ELK, SPLUNK, SUMO, LOGZ, JENKINS, BAMBOO, DOCKER, NEXUS, ARTIFACTORY, SMB,
      SFTP, AMAZON_S3_HELM_REPO, GCS_HELM_REPO, HTTP_HELM_REPO, AZURE_ARTIFACTS_PAT, GIT, SMTP, JIRA, SERVICENOW);

  @Inject private SecretManager secretManager;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private FeatureFlagService featureFlagService;

  public boolean hasReferencedSecrets(SettingAttribute settingAttribute) {
    // Only use referenced secrets feature if the feature flag is on and the setting attribute type supports it.
    if (settingAttribute == null || settingAttribute.getValue() == null || settingAttribute.getAccountId() == null
        || settingAttribute.getValue().getSettingType() == null
        || !featureFlagService.isEnabled(FeatureName.CONNECTORS_REF_SECRETS, settingAttribute.getAccountId())) {
      return false;
    }

    if (ATTRIBUTES_USING_REFERENCES.contains(settingAttribute.getValue().getSettingType())) {
      settingAttribute.setSecretsMigrated(true);
      return true;
    }
    settingAttribute.setSecretsMigrated(false);
    return false;
  }

  public void updateEncryptedFieldsInResponse(SettingAttribute settingAttribute, boolean maskEncryptedFields) {
    SettingValue settingValue = settingAttribute.getValue();
    if (settingValue instanceof EncryptableSetting) {
      if (hasReferencedSecrets(settingAttribute)) {
        // Copy encrypted ref field values (which contain IDs) to the encrypted fields for UI consumption. Don't do
        // masking.
        copyFromEncryptedRefFields((EncryptableSetting) settingValue);
      } else if (maskEncryptedFields) {
        // Mask encrypted fields.
        secretManager.maskEncryptedFields((EncryptableSetting) settingValue);
      }
    }
  }

  public void updateReferencedSecrets(SettingAttribute settingAttribute) {
    if (!hasReferencedSecrets(settingAttribute)) {
      return;
    }

    SettingValue settingValue = settingAttribute.getValue();
    if (!(settingValue instanceof EncryptableSetting)) {
      return;
    }

    EncryptableSetting object = (EncryptableSetting) settingValue;
    boolean wasDecrypted = object.isDecrypted();
    object.setDecrypted(false);

    // The encrypted field contains the encrypted text id. Copy that value to the encrypted ref field and set encrypted
    // field to null.
    copyToEncryptedRefFields(object);
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(object, settingAttribute.getAppId(), null);
    managerDecryptionService.decrypt(object, encryptionDetails);

    if (wasDecrypted) {
      // Set decrypted to true even if the call to decrypt did not and the setting value initially had decrypted set to
      // true.
      object.setDecrypted(true);
    }
  }

  /**
   * resetEncryptedFields sets encrypted fields to null. This method is required before saving/updating setting
   * attributes because wingsPersistence might try to encrypt the values stored in encrypted fields into the the ref
   * fields for legacy reasons.
   *
   * It does the following type of conversion:
   * { "password": "val1", "encryptedPassword": "..." }
   * ->
   * { "password": null, "encryptedPassword": "..." }
   *
   * @param object the encryptable setting to mutate
   */
  public void resetEncryptedFields(EncryptableSetting object) {
    List<Field> encryptedFields = object.getEncryptedFields();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        f.set(object, null);
      }
    } catch (Exception e) {
      throw new InvalidRequestException(REFERENCED_SECRET_ERROR_MSG, e);
    }
  }

  /**
   * copyToEncryptedRefFields copies the value of encrypted fields to encrypted ref fields. This method is needed
   * because UI passes the ID in the encrypted field and not the ref field.
   *
   * It does the following type of conversion:
   * { "password": "val1", "encryptedPassword": "..." }
   * ->
   * { "password": null, "encryptedPassword": "val1" }
   *
   * @param object the encryptable setting to mutate
   */
  public void copyToEncryptedRefFields(EncryptableSetting object) {
    List<Field> encryptedFields = object.getEncryptedFields();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        char[] fieldValue = (char[]) f.get(object);
        if (fieldValue == null) {
          // Ignore if encrypted field value is null. This is required for yaml.
          continue;
        }
        Field encryptedRefField = getEncryptedRefField(f, object);
        encryptedRefField.setAccessible(true);
        encryptedRefField.set(object, String.valueOf(fieldValue));
        f.set(object, null);
      }
    } catch (Exception e) {
      throw new InvalidRequestException(REFERENCED_SECRET_ERROR_MSG, e);
    }
  }

  /**
   * copyFromEncryptedRefFields copies the value of encrypted ref fields to encrypted fields. This method is needed
   * because UI passes the ID in the encrypted field and not the ref field.
   *
   * It does the following type of conversion:
   * { "password": "...", "encryptedPassword": "val1" }
   * ->
   * { "password": "val1", "encryptedPassword": null }
   *
   * @param object the encryptable setting to mutate
   */
  public void copyFromEncryptedRefFields(EncryptableSetting object) {
    List<Field> encryptedFields = object.getEncryptedFields();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        Field encryptedRefField = getEncryptedRefField(f, object);
        encryptedRefField.setAccessible(true);
        String encryptedFieldValue = (String) encryptedRefField.get(object);
        f.set(object, encryptedFieldValue == null ? null : encryptedFieldValue.toCharArray());
        encryptedRefField.set(object, null);
      }
    } catch (Exception e) {
      throw new InvalidRequestException(REFERENCED_SECRET_ERROR_MSG, e);
    }
  }

  boolean isConnectorCategory(SettingAttribute.SettingCategory settingCategory) {
    return settingCategory == CONNECTOR || settingCategory == HELM_REPO || settingCategory == AZURE_ARTIFACTS;
  }

  boolean isArtifactServer(SettingValue.SettingVariableTypes settingVariableTypes) {
    switch (settingVariableTypes) {
      case JENKINS:
      case BAMBOO:
      case DOCKER:
      case NEXUS:
      case ARTIFACTORY:
      case SMB:
      case SFTP:
      case AMAZON_S3_HELM_REPO:
      case GCS_HELM_REPO:
      case HTTP_HELM_REPO:
      case AZURE_ARTIFACTS_PAT:
        return true;
      default:
        return false;
    }
  }

  public static List<String> getAllEncryptedSecrets(SettingValue obj) {
    if (!(obj instanceof EncryptableSetting)) {
      return Collections.emptyList();
    }

    List<Field> encryptedFields = EncryptionReflectUtils.getEncryptedFields(obj.getClass());
    if (EmptyPredicate.isEmpty(encryptedFields)) {
      return Collections.emptyList();
    }

    List<String> encryptedSecrets = new ArrayList<>();
    for (Field encryptedField : encryptedFields) {
      Field encryptedRefField = EncryptionReflectUtils.getEncryptedRefField(encryptedField, (Encryptable) obj);
      encryptedRefField.setAccessible(true);
      try {
        String encryptedValue = (String) encryptedRefField.get(obj);
        encryptedSecrets.add(encryptedValue);
      } catch (IllegalAccessException e) {
        throw new InvalidRequestException("Unable to access encrypted field", e);
      }
    }
    return encryptedSecrets;
  }
}
