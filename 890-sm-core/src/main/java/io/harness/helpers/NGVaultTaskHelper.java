/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.helpers;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.helpers.ext.vault.VaultAppRoleLoginRequest;
import io.harness.helpers.ext.vault.VaultAppRoleLoginResponse;
import io.harness.helpers.ext.vault.VaultAppRoleLoginResult;
import io.harness.helpers.ext.vault.VaultRestClientFactory;
import io.harness.helpers.ext.vault.VaultSysAuthRestClient;

import software.wings.beans.BaseVaultConfig;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@UtilityClass
@OwnedBy(PL)
@Slf4j
public class NGVaultTaskHelper {
  public static VaultAppRoleLoginResult getVaultAppRoleLoginResult(BaseVaultConfig vaultConfig) {
    try {
      VaultSysAuthRestClient restClient =
          VaultRestClientFactory.getVaultRetrofit(vaultConfig.getVaultUrl(), vaultConfig.isCertValidationRequired())
              .create(VaultSysAuthRestClient.class);

      VaultAppRoleLoginRequest loginRequest = VaultAppRoleLoginRequest.builder()
                                                  .roleId(vaultConfig.getAppRoleId())
                                                  .secretId(vaultConfig.getSecretId())
                                                  .build();
      Response<VaultAppRoleLoginResponse> response =
          restClient.appRoleLogin(vaultConfig.getNamespace(), loginRequest).execute();

      VaultAppRoleLoginResult result = null;
      if (response.isSuccessful()) {
        result = response.body().getAuth();
      } else {
        logAndThrowVaultError(vaultConfig, response, "AppRole Based Login");
      }
      return result;
    } catch (IOException e) {
      String message = "NG: Failed to perform AppRole based login for secret manager " + vaultConfig.getName() + " at "
          + vaultConfig.getVaultUrl();
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
    }
  }

  public static void logAndThrowVaultError(BaseVaultConfig baseVaultConfig, Response response, String operation)
      throws IOException {
    if (baseVaultConfig == null || response == null) {
      return;
    }
    String errorMsg = "";
    if (response.errorBody() != null) {
      errorMsg =
          String.format("Failed to %s for Vault: %s And Namespace: %s due to the following error from vault: \"%s\".",
              operation, baseVaultConfig.getName(), baseVaultConfig.getNamespace(), response.errorBody().string());
    } else {
      errorMsg = String.format(
          "Failed to %s for Vault: %s And Namespace: %s due to the following error from vault: \"%s\".", operation,
          baseVaultConfig.getName(), baseVaultConfig.getNamespace(), response.message() + response.body());
    }
    log.error(errorMsg);
    throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, errorMsg, USER);
  }

  public static String getToken(BaseVaultConfig vaultConfig) {
    if (vaultConfig.isUseVaultAgent()) {
      try {
        byte[] content = Files.readAllBytes(Paths.get(URI.create("file://" + vaultConfig.getSinkPath())));
        String token = new String(content);
        vaultConfig.setAuthToken(token);
        return token;
      } catch (IOException e) {
        throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR,
            "Using Vault Agent Cannot read Token From Sink Path:" + vaultConfig.getSinkPath(), e, USER);
      }
    } else {
      return vaultConfig.getAuthToken();
    }
  }
}
