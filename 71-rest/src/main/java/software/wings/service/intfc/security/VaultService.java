package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretChangeLog;
import io.harness.helpers.ext.vault.SecretEngineSummary;
import io.harness.helpers.ext.vault.VaultAppRoleLoginResult;

import software.wings.beans.VaultConfig;
import software.wings.beans.alert.KmsSetupAlert;

import java.util.List;

/**
 * Created by rsingh on 11/2/17.
 */
@OwnedBy(PL)
public interface VaultService {
  String saveOrUpdateVaultConfig(String accountId, VaultConfig vaultConfig, boolean validate);

  boolean deleteVaultConfig(String accountId, String vaultConfigId);

  List<SecretEngineSummary> listSecretEngines(VaultConfig vaultConfig);

  void decryptVaultConfigSecrets(String accountId, VaultConfig vaultConfig, boolean maskSecret);

  VaultConfig getVaultConfig(String accountId, String entityId);

  VaultConfig getVaultConfigByName(String accountId, String name);

  void renewToken(VaultConfig vaultConfig);

  void renewAppRoleClientToken(VaultConfig vaultConfig);

  List<SecretChangeLog> getVaultSecretChangeLogs(EncryptedData encryptedData, VaultConfig vaultConfig);

  KmsSetupAlert getRenewalAlert(VaultConfig vaultConfig);

  void validateVaultConfig(String accountId, VaultConfig vaultConfig);

  VaultAppRoleLoginResult appRoleLogin(VaultConfig vaultConfig);
}
