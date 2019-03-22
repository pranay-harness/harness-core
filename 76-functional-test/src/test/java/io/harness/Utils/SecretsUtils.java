package io.harness.Utils;

import software.wings.beans.VaultConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import java.util.List;

public class SecretsUtils {
  public static boolean isVaultAvailable(List<VaultConfig> vaultList, String vaultName) {
    if (vaultList == null || vaultList.size() == 0) {
      return false;
    }
    for (VaultConfig vaultConfig : vaultList) {
      if (vaultConfig.getName().equals(vaultName)) {
        return true;
      }
    }
    return false;
  }

  public static SecretText createSecretTextObject(String textName, String textValue) {
    //    GenericEntityFilter appFilter =
    //        GenericEntityFilter.builder().filterType(FilterType.ALL).ids(null).build();
    //    Set<String> filterTypes = new HashSet<>();
    //    filterTypes.add("NON_PROD");
    //    EnvFilter envFilter = EnvFilter.builder().filterTypes(filterTypes).ids(null).build();
    //
    //    AppEnvRestriction appEnvRestriction =
    //        AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
    //
    //    Set<AppEnvRestriction> appEnvRestrictionSet = new HashSet<>();
    //    appEnvRestrictionSet.add(appEnvRestriction);
    //    UsageRestrictions usageRestrictions =
    //    UsageRestrictions.builder().appEnvRestrictions(appEnvRestrictionSet).build();
    SecretText secretText = SecretText.builder().name(textName).value(textValue).build();
    return secretText;
  }

  public static boolean isSecretAvailable(List<EncryptedData> secretList, String secretName) {
    if (secretList == null || secretList.size() == 0) {
      return false;
    }

    for (EncryptedData encryptedData : secretList) {
      if (encryptedData.getName().equals(secretName)) {
        return true;
      }
    }
    return false;
  }

  public static String getValueFromName(
      SecretManagementDelegateService secretManagementDelegateService, EncryptedData data, VaultConfig vaultConfig) {
    return new String(secretManagementDelegateService.decrypt(data, vaultConfig));
  }
}
