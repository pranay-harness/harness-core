/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
