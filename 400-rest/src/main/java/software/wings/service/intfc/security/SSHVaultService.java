/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.helpers.ext.vault.SSHVaultAuthResult;
import io.harness.helpers.ext.vault.SecretEngineSummary;

import software.wings.beans.SSHVaultConfig;

import java.util.List;

@OwnedBy(PL)
public interface SSHVaultService {
  SSHVaultConfig getSSHVaultConfig(String accountId, String entityId);

  String saveOrUpdateSSHVaultConfig(String accountId, SSHVaultConfig sshVaultConfig, boolean validate);

  boolean deleteSSHVaultConfig(String accountId, String sshVaultConfigId);

  SSHVaultAuthResult sshVaultAuthResult(SSHVaultConfig sshVaultConfig);

  List<SecretEngineSummary> listSecretEngines(SSHVaultConfig sshVaultConfig);

  void decryptVaultConfigSecrets(String accountId, SSHVaultConfig sshVaultConfig, boolean maskSecret);
}
