package software.wings.security.encryption.setupusage.builders;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.encryption.EncryptionReflectUtils;
import lombok.NonNull;
import software.wings.beans.FeatureName;
import software.wings.beans.SecretManagerConfig;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.EncryptionDetail;
import software.wings.security.encryption.setupusage.SecretSetupUsage;
import software.wings.security.encryption.setupusage.SecretSetupUsageBuilder;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class SecretManagerSetupUsageBuilder implements SecretSetupUsageBuilder {
  @Inject private SecretManagerConfigService secretManagerConfigService;
  @Inject private FeatureFlagService featureFlagService;

  public Set<SecretSetupUsage> buildSecretSetupUsages(@NonNull String accountId, String secretId,
      @NonNull Map<String, Set<EncryptedDataParent>> parentsByParentIds, @NonNull EncryptionDetail encryptionDetail) {
    Set<String> parentIds = parentsByParentIds.keySet();
    List<SecretManagerConfig> secretManagerConfigList =
        secretManagerConfigService.listSecretManagers(accountId, true, false)
            .stream()
            .filter(secretManagerConfig -> parentIds.contains(secretManagerConfig.getUuid()))
            .collect(Collectors.toList());

    Set<SecretSetupUsage> secretSetupUsages = new HashSet<>();
    for (SecretManagerConfig secretsManagerConfig : secretManagerConfigList) {
      secretsManagerConfig.setEncryptedBy(encryptionDetail.getSecretManagerName());
      if (featureFlagService.isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, secretsManagerConfig.getAccountId())) {
        Set<EncryptedDataParent> parents = parentsByParentIds.get(secretsManagerConfig.getUuid());
        parents.forEach(parent -> {
          List<Field> encryptedFields = EncryptionReflectUtils.getEncryptedFields(secretsManagerConfig.getClass());
          EncryptionReflectUtils.getFieldHavingFieldName(encryptedFields, parent.getFieldName()).ifPresent(field -> {
            secretSetupUsages.add(SecretSetupUsage.builder()
                                      .entityId(parent.getId())
                                      .type(parent.getType())
                                      .fieldName(field.getName())
                                      .entity(secretsManagerConfig)
                                      .build());
          });
        });
      } else {
        secretSetupUsages.add(
            SecretSetupUsage.builder()
                .entityId(secretsManagerConfig.getUuid())
                .type(SettingVariableTypes.valueOf(secretsManagerConfig.getEncryptionType().toString()))
                .entity(secretsManagerConfig)
                .build());
      }
    }
    return secretSetupUsages;
  }
}