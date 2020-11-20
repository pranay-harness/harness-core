package io.harness.secrets.setupusage.builders;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedDataParent;
import io.harness.beans.SecretManagerConfig;
import io.harness.encryption.EncryptionReflectUtils;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.secrets.setupusage.EncryptionDetail;
import io.harness.secrets.setupusage.SecretSetupUsage;
import io.harness.secrets.setupusage.SecretSetupUsageBuilder;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(PL)
@Singleton
public class SecretManagerSetupUsageBuilder implements SecretSetupUsageBuilder {
  @Inject private SecretManagerConfigService secretManagerConfigService;

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
    }
    return secretSetupUsages;
  }

  @Override
  public Map<String, Set<String>> buildAppEnvMap(
      String accountId, String secretId, Map<String, Set<EncryptedDataParent>> parentsByParentIds) {
    return Collections.emptyMap();
  }
}
