/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.EncryptedData;
import io.harness.beans.SecretText;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.VaultConfig;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.UsageRestrictions;
import software.wings.security.UsageRestrictions.AppEnvRestriction;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SecretsUtils {
  public static boolean isVaultAvailable(List<VaultConfig> vaultList, String vaultName) {
    if (isEmpty(vaultList)) {
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
    if (isEmpty(secretList)) {
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
      EncryptionService encryptionService, EncryptedData data, VaultConfig vaultConfig) {
    return new String(encryptionService.getDecryptedValue(EncryptedDataDetail.builder()
                                                              .encryptedData(SecretManager.buildRecordData(data))
                                                              .encryptionConfig(vaultConfig)
                                                              .build(),
        false));
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
