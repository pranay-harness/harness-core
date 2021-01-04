package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretChangeLog;
import io.harness.helpers.ext.vault.SecretEngineSummary;
import io.harness.helpers.ext.vault.VaultAppRoleLoginResult;
import io.harness.security.encryption.EncryptedRecord;

import software.wings.beans.CyberArkConfig;
import software.wings.beans.TaskType;
import software.wings.beans.VaultConfig;
import software.wings.delegatetasks.DelegateTaskType;

import java.util.List;

/**
 * Created by rsingh on 10/2/17.
 */
@OwnedBy(PL)
public interface SecretManagementDelegateService {
  int NUM_OF_RETRIES = 3;

  /**
   * Retrieve the versions metadata for Vault managed secrets from Hashicorp Vault, and construct the version history as
   * {@see SecretChangeLog} to be displayed in Harness UI.
   */
  @DelegateTaskType(TaskType.VAULT_GET_CHANGELOG)
  List<SecretChangeLog> getVaultSecretChangeLogs(EncryptedRecord encryptedData, VaultConfig vaultConfig);

  /**
   * Renew the Hashicorp Vault authentication token.
   */
  @DelegateTaskType(TaskType.VAULT_RENEW_TOKEN) boolean renewVaultToken(VaultConfig vaultConfig);

  /**
   * List vault secret engines
   */
  @DelegateTaskType(TaskType.VAULT_LIST_ENGINES) List<SecretEngineSummary> listSecretEngines(VaultConfig vaultConfig);

  /**
   * Login Vault using AppRole auth.
   */
  @DelegateTaskType(TaskType.VAULT_APPROLE_LOGIN) VaultAppRoleLoginResult appRoleLogin(VaultConfig vaultConfig);

  /**
   * Validate the CyberArk configuration, including the connectivity to CyberArk service, client certificate etc.
   */
  @DelegateTaskType(TaskType.CYBERARK_VALIDATE_CONFIG) boolean validateCyberArkConfig(CyberArkConfig cyberArkConfig);
}
