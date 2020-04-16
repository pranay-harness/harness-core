package software.wings.security.encryption.setupusage.builders;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.SearchFilter;
import io.harness.encryption.EncryptionReflectUtils;
import lombok.NonNull;
import software.wings.beans.FeatureName;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.EncryptionDetail;
import software.wings.security.encryption.setupusage.SecretSetupUsage;
import software.wings.security.encryption.setupusage.SecretSetupUsageBuilder;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class SettingAttributeSetupUsageBuilder implements SecretSetupUsageBuilder {
  @Inject private SettingsService settingsService;
  @Inject private FeatureFlagService featureFlagService;

  public Set<SecretSetupUsage> buildSecretSetupUsages(@NonNull String accountId, String secretId,
      @NonNull Map<String, Set<EncryptedDataParent>> parentsByParentIds, @NonNull EncryptionDetail encryptionDetail) {
    Set<String> parentIds = parentsByParentIds.keySet();
    List<SettingAttribute> settingAttributeList =
        settingsService
            .list(aPageRequest()
                      .addFilter(ID_KEY, SearchFilter.Operator.IN, parentIds.toArray())
                      .addFilter(ACCOUNT_ID_KEY, SearchFilter.Operator.EQ, accountId)
                      .build(),
                null, null)
            .getResponse();

    Set<SecretSetupUsage> secretSetupUsages = new HashSet<>();
    for (SettingAttribute settingAttribute : settingAttributeList) {
      settingAttribute.setEncryptionType(encryptionDetail.getEncryptionType());
      settingAttribute.setEncryptedBy(encryptionDetail.getSecretManagerName());
      if (featureFlagService.isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, settingAttribute.getAccountId())) {
        Set<EncryptedDataParent> parents = parentsByParentIds.get(settingAttribute.getUuid());
        parents.forEach(parent -> {
          List<Field> encryptedFields = settingAttribute.getValue().getEncryptedFields();
          EncryptionReflectUtils.getFieldHavingFieldName(encryptedFields, parent.getFieldName()).ifPresent(field -> {
            secretSetupUsages.add(SecretSetupUsage.builder()
                                      .entityId(parent.getId())
                                      .type(parent.getType())
                                      .fieldName(SettingAttributeKeys.value.concat(".").concat(field.getName()))
                                      .entity(settingAttribute)
                                      .build());
          });
        });
      } else {
        secretSetupUsages.add(SecretSetupUsage.builder()
                                  .entityId(settingAttribute.getUuid())
                                  .type(settingAttribute.getValue().getSettingType())
                                  .entity(settingAttribute)
                                  .build());
      }
    }
    return secretSetupUsages;
  }
}
