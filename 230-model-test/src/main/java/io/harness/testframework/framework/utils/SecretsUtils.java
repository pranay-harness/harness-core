package io.harness.testframework.framework.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.harness.beans.EnvFilter;
import io.harness.beans.GenericEntityFilter;
import io.harness.beans.GenericEntityFilter.FilterType;
import io.harness.beans.UsageRestrictions;
import io.harness.beans.UsageRestrictions.AppEnvRestriction;
import software.wings.beans.VaultConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    return SecretText.builder().name(textName).value(textValue).build();
  }

  public static SecretText createSecretTextObjectWithUsageRestriction(
      String textName, String textValue, String envType) {
    createUsageRestrictions(envType);
    return SecretText.builder()
        .name(textName)
        .value(textValue)
        .usageRestrictions(createUsageRestrictions(envType))
        .build();
  }

  public static UsageRestrictions createUsageRestrictions(String envType) {
    GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).ids(null).build();

    Set<String> filterTypes = new HashSet<>();
    filterTypes.add(envType);
    EnvFilter envFilter = EnvFilter.builder().filterTypes(filterTypes).ids(null).build();
    AppEnvRestriction appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();

    Set<AppEnvRestriction> appEnvRestrictionSet = new HashSet<>();
    appEnvRestrictionSet.add(appEnvRestriction);
    return UsageRestrictions.builder().appEnvRestrictions(appEnvRestrictionSet).build();
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

  public static JsonElement getUsageRestDataAsJson(SecretText secretText) {
    Gson gson = new Gson();
    String usageRestrictionJson = gson.toJson(secretText);
    JsonElement jsonElement = gson.fromJson(usageRestrictionJson, JsonElement.class);
    ((JsonObject) jsonElement)
        .get("usageRestrictions")
        .getAsJsonObject()
        .get("appEnvRestrictions")
        .getAsJsonArray()
        .get(0)
        .getAsJsonObject()
        .get("appFilter")
        .getAsJsonObject()
        .addProperty("type", "GenericEntityFilter");

    ((JsonObject) jsonElement)
        .get("usageRestrictions")
        .getAsJsonObject()
        .get("appEnvRestrictions")
        .getAsJsonArray()
        .get(0)
        .getAsJsonObject()
        .get("envFilter")
        .getAsJsonObject()
        .addProperty("type", "EnvFilter");

    return jsonElement;
  }
}
