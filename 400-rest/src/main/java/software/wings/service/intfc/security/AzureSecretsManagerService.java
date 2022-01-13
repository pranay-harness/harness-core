/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.AzureVaultConfig;

import java.util.List;

@OwnedBy(PL)
public interface AzureSecretsManagerService {
  String saveAzureSecretsManagerConfig(String accountId, AzureVaultConfig secretsManagerConfig);

  List<String> listAzureVaults(String accountId, AzureVaultConfig secretsManagerConfig);

  void decryptAzureConfigSecrets(AzureVaultConfig secretManagerConfig, boolean maskSecret);

  AzureVaultConfig getEncryptionConfig(String accountId, String id);

  void validateAzureSecretsManagerConfig(String accountId, AzureVaultConfig secretsManagerConfig);

  boolean deleteConfig(String accountId, String configId);
}
