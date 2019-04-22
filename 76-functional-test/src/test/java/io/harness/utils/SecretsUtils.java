package io.harness.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import software.wings.beans.VaultConfig;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;

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
    SecretText secretText = SecretText.builder().name(textName).value(textValue).build();
    return secretText;
  }

  public static SecretText createSecretTextObjectWithUsageRestriction(
      String textName, String textValue, String envType) {
    createUsageRestrictions(envType);
    SecretText secretText = SecretText.builder()
                                .name(textName)
                                .value(textValue)
                                .usageRestrictions(createUsageRestrictions(envType))
                                .build();
    return secretText;
  }

  public static UsageRestrictions createUsageRestrictions(String envType) {
    GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).ids(null).build();

    Set<String> filterTypes = new HashSet<>();
    filterTypes.add(envType);
    EnvFilter envFilter = EnvFilter.builder().filterTypes(filterTypes).ids(null).build();
    AppEnvRestriction appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();

    Set<AppEnvRestriction> appEnvRestrictionSet = new HashSet<>();
    appEnvRestrictionSet.add(appEnvRestriction);
    UsageRestrictions usageRestrictions = UsageRestrictions.builder().appEnvRestrictions(appEnvRestrictionSet).build();
    return usageRestrictions;
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
