package software.wings.service.impl;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.encryption.EncryptionReflectUtils.getEncryptedRefField;

import static software.wings.beans.SettingAttribute.SettingCategory.AZURE_ARTIFACTS;
import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;
import static software.wings.beans.SettingAttribute.SettingCategory.HELM_REPO;
import static software.wings.security.PermissionAttribute.PermissionType;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CLOUD_PROVIDERS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CONNECTORS;
import static software.wings.settings.SettingVariableTypes.AMAZON_S3_HELM_REPO;
import static software.wings.settings.SettingVariableTypes.APM_VERIFICATION;
import static software.wings.settings.SettingVariableTypes.APP_DYNAMICS;
import static software.wings.settings.SettingVariableTypes.ARTIFACTORY;
import static software.wings.settings.SettingVariableTypes.AWS;
import static software.wings.settings.SettingVariableTypes.AZURE;
import static software.wings.settings.SettingVariableTypes.AZURE_ARTIFACTS_PAT;
import static software.wings.settings.SettingVariableTypes.BAMBOO;
import static software.wings.settings.SettingVariableTypes.BUG_SNAG;
import static software.wings.settings.SettingVariableTypes.CLOUD_WATCH;
import static software.wings.settings.SettingVariableTypes.DATA_DOG;
import static software.wings.settings.SettingVariableTypes.DATA_DOG_LOG;
import static software.wings.settings.SettingVariableTypes.DOCKER;
import static software.wings.settings.SettingVariableTypes.DYNA_TRACE;
import static software.wings.settings.SettingVariableTypes.ELK;
import static software.wings.settings.SettingVariableTypes.GCP;
import static software.wings.settings.SettingVariableTypes.GCS_HELM_REPO;
import static software.wings.settings.SettingVariableTypes.GIT;
import static software.wings.settings.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingVariableTypes.HTTP_HELM_REPO;
import static software.wings.settings.SettingVariableTypes.INSTANA;
import static software.wings.settings.SettingVariableTypes.JENKINS;
import static software.wings.settings.SettingVariableTypes.JIRA;
import static software.wings.settings.SettingVariableTypes.KUBERNETES_CLUSTER;
import static software.wings.settings.SettingVariableTypes.LOGZ;
import static software.wings.settings.SettingVariableTypes.NEW_RELIC;
import static software.wings.settings.SettingVariableTypes.NEXUS;
import static software.wings.settings.SettingVariableTypes.PCF;
import static software.wings.settings.SettingVariableTypes.PROMETHEUS;
import static software.wings.settings.SettingVariableTypes.SERVICENOW;
import static software.wings.settings.SettingVariableTypes.SFTP;
import static software.wings.settings.SettingVariableTypes.SMB;
import static software.wings.settings.SettingVariableTypes.SMTP;
import static software.wings.settings.SettingVariableTypes.SPLUNK;
import static software.wings.settings.SettingVariableTypes.SPOT_INST;
import static software.wings.settings.SettingVariableTypes.SUMO;
import static software.wings.settings.SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES;

import io.harness.beans.Encryptable;
import io.harness.beans.FeatureName;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.EncryptionReflectUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedUsageRestrictionsException;
import io.harness.exception.WingsException;
import io.harness.reflection.ReflectionUtils;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SettingAttribute;
import software.wings.security.UsageRestrictions;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;
import software.wings.yaml.YamlHelper;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

@Singleton
public class SettingServiceHelper {
  private static final String REFERENCED_SECRET_ERROR_MSG = "Unable to copy encryption details";
  private static final String USE_ENCRYPTED_VALUE_FLAG_FIELD_BASE = "useEncrypted";
  public static final Set<SettingVariableTypes> ATTRIBUTES_USING_REFERENCES = Sets.immutableEnumSet(AWS, AZURE, GCP,
      KUBERNETES_CLUSTER, PCF, SPOT_INST, APP_DYNAMICS, NEW_RELIC, INSTANA, PROMETHEUS, DATA_DOG, DYNA_TRACE,
      CLOUD_WATCH, DATA_DOG_LOG, BUG_SNAG, ELK, SPLUNK, SUMO, LOGZ, APM_VERIFICATION, JENKINS, BAMBOO, DOCKER, NEXUS,
      ARTIFACTORY, SMB, SFTP, AMAZON_S3_HELM_REPO, GCS_HELM_REPO, HTTP_HELM_REPO, AZURE_ARTIFACTS_PAT, GIT, SMTP, JIRA,
      SERVICENOW, WINRM_CONNECTION_ATTRIBUTES, HOST_CONNECTION_ATTRIBUTES);

  @Inject private SecretManager secretManager;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private UsageRestrictionsService usageRestrictionsService;

  public boolean hasReferencedSecrets(SettingAttribute settingAttribute) {
    if (settingAttribute == null || settingAttribute.getValue() == null || settingAttribute.getAccountId() == null
        || settingAttribute.getValue().getSettingType() == null) {
      return false;
    }

    boolean isSecretsReferenceAllowed =
        featureFlagService.isEnabled(FeatureName.CONNECTORS_REF_SECRETS, settingAttribute.getAccountId())
        && ATTRIBUTES_USING_REFERENCES.contains(settingAttribute.getValue().getSettingType());
    if (!isSecretsReferenceAllowed) {
      return false;
    }

    if (settingAttribute.getUuid() == null) {
      settingAttribute.setSecretsMigrated(true);
    }
    return settingAttribute.isSecretsMigrated();
  }

  public void updateSettingAttributeBeforeResponse(SettingAttribute settingAttribute, boolean maskEncryptedFields) {
    if (settingAttribute == null) {
      return;
    }

    // Update usage restrictions if referenced secrets are used.
    updateUsageRestrictions(settingAttribute);
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

  boolean isArtifactServer(SettingVariableTypes settingVariableTypes) {
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

  public static List<Field> getAllEncryptedFields(SettingValue obj) {
    if (!(obj instanceof EncryptableSetting)) {
      return Collections.emptyList();
    }

    return EncryptionReflectUtils.getEncryptedFields(obj.getClass())
        .stream()
        .filter(field -> {
          if (EncryptionReflectUtils.isSecretReference(field)) {
            String flagFiledName = USE_ENCRYPTED_VALUE_FLAG_FIELD_BASE + StringUtils.capitalize(field.getName());

            List<Field> declaredAndInheritedFields =
                ReflectionUtils.getDeclaredAndInheritedFields(obj.getClass(), f -> f.getName().equals(flagFiledName));
            if (isNotEmpty(declaredAndInheritedFields)) {
              Object flagFieldValue = ReflectionUtils.getFieldValue(obj, declaredAndInheritedFields.get(0));
              return flagFieldValue != null && (Boolean) flagFieldValue;
            }
          }

          return true;
        })
        .collect(Collectors.toList());
  }

  public static List<String> getAllEncryptedSecrets(SettingValue obj) {
    if (!(obj instanceof EncryptableSetting)) {
      return Collections.emptyList();
    }

    List<Field> encryptedFields = SettingServiceHelper.getAllEncryptedFields(obj);
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

  public void updateUsageRestrictions(SettingAttribute settingAttribute) {
    if (isNotEmpty(getUsedSecretIds(settingAttribute))) {
      settingAttribute.setUsageRestrictions(null);
    }
  }

  public UsageRestrictions getUsageRestrictions(SettingAttribute settingAttribute) {
    if (isNotEmpty(getUsedSecretIds(settingAttribute))) {
      return null;
    }

    return settingAttribute.getUsageRestrictions();
  }

  public void validateUsageRestrictionsOnEntitySave(
      SettingAttribute settingAttribute, String accountId, UsageRestrictions newUsageRestrictions) {
    Set<String> usedSecretIds = getUsedSecretIds(settingAttribute);
    if (isNotEmpty(usedSecretIds)) {
      if (!secretManager.hasUpdateAccessToSecrets(usedSecretIds, accountId)) {
        throw new UnauthorizedUsageRestrictionsException(WingsException.USER);
      }
      return;
    }
    PermissionType permissionType = getPermissionType(settingAttribute);
    usageRestrictionsService.validateUsageRestrictionsOnEntitySave(
        accountId, permissionType, newUsageRestrictions, false);
  }

  public void validateUsageRestrictionsOnEntityUpdate(SettingAttribute settingAttribute, String accountId,
      UsageRestrictions oldUsageRestrictions, UsageRestrictions newUsageRestrictions) {
    Set<String> usedSecretIds = getUsedSecretIds(settingAttribute);
    if (isNotEmpty(usedSecretIds)) {
      if (!secretManager.hasUpdateAccessToSecrets(usedSecretIds, accountId)) {
        throw new UnauthorizedUsageRestrictionsException(WingsException.USER);
      }
      return;
    }
    PermissionType permissionType = getPermissionType(settingAttribute);
    usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(
        accountId, permissionType, oldUsageRestrictions, newUsageRestrictions, false);
  }

  public boolean userHasPermissionsToChangeEntity(
      SettingAttribute settingAttribute, String accountId, UsageRestrictions entityUsageRestrictions) {
    Set<String> usedSecretIds = getUsedSecretIds(settingAttribute);
    if (isNotEmpty(usedSecretIds)) {
      return secretManager.hasUpdateAccessToSecrets(usedSecretIds, accountId);
    }

    PermissionType permissionType = getPermissionType(settingAttribute);
    return usageRestrictionsService.userHasPermissionsToChangeEntity(
        accountId, permissionType, entityUsageRestrictions, false);
  }

  public Set<String> getUsedSecretIds(SettingAttribute settingAttribute) {
    if (hasReferencedSecrets(settingAttribute)) {
      List<String> secretIds = emptyIfNull(settingAttribute.fetchRelevantSecretIds());
      return secretIds.stream()
          .filter(secretId -> isNotEmpty(secretId) && !YamlHelper.ENCRYPTED_VALUE_STR.equals(secretId))
          .collect(Collectors.toSet());
    }

    return null;
  }

  public PermissionType getPermissionType(SettingAttribute settingAttribute) {
    if (settingAttribute == null || settingAttribute.getValue() == null
        || settingAttribute.getValue().getType() == null) {
      return ACCOUNT_MANAGEMENT;
    }
    switch (SettingAttribute.SettingCategory.getCategory(
        SettingVariableTypes.valueOf(settingAttribute.getValue().getType()))) {
      case AZURE_ARTIFACTS:
      case HELM_REPO:
      case CONNECTOR: {
        return MANAGE_CONNECTORS;
      }
      case CLOUD_PROVIDER: {
        return MANAGE_CLOUD_PROVIDERS;
      }
      default: {
        return ACCOUNT_MANAGEMENT;
      }
    }
  }
}
