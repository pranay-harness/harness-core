package io.harness.functional.secrets;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.utils.SecretsUtils;
import io.harness.testframework.restutils.SecretsRestUtils;
import io.harness.testframework.restutils.VaultRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.VaultConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.service.impl.security.VaultServiceImpl;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import java.io.IOException;
import java.util.List;

/**
 * @author marklu on 2019-04-25
 */
@Slf4j
public class VaultServiceFunctionalTest extends AbstractFunctionalTest {
  private static final String APPROLE_ID = "796a7ec5-04f6-383b-dad4-67a1677da6ff";
  private static final String APPROLE_VAULT_NAME = "Test Vault AppRole";
  private static final String QA_VAULT_URL = "https://vaultqa.harness.io";

  @Inject private SecretManagementDelegateService secretManagementDelegateService;
  @Inject private VaultServiceImpl vaultService;

  @Test
  @Category(FunctionalTests.class)
  @Ignore
  public void createVaultWithAppRoleAuth() throws IOException {
    VaultConfig vaultConfigWithAppRoleSecret =
        VaultConfig.builder()
            .accountId(getAccount().getUuid())
            .name(APPROLE_VAULT_NAME)
            .vaultUrl(QA_VAULT_URL)
            .appRoleId(APPROLE_ID)
            .secretId(new ScmSecret().decryptToString(new SecretName("qa_vault_approle_secret_id")))
            .basePath("/foo2/bar2")
            .secretEngineVersion(2)
            .isDefault(true)
            .build();

    String appRoleVaultId = VaultRestUtils.addVault(bearerToken, vaultConfigWithAppRoleSecret);
    assertNotNull(appRoleVaultId);
    logger.info("AppRole based Vault secret manager created.");
    List<VaultConfig> vaultConfigs = SecretsRestUtils.getListConfigs(getAccount().getUuid(), bearerToken);
    assertTrue(SecretsUtils.isVaultAvailable(vaultConfigs, APPROLE_VAULT_NAME));

    try {
      String secretName = "MySecret";
      String secretValue = "MyValue";
      SecretText secretText = SecretsUtils.createSecretTextObject(secretName, secretValue);

      String secretsId = SecretsRestUtils.addSecret(getAccount().getUuid(), bearerToken, secretText);
      assertTrue(StringUtils.isNotBlank(secretsId));

      secretName = "MySecret-Updated";
      secretValue = "MyValue-Updated";
      secretText.setName(secretName);
      secretText.setValue(secretValue);
      boolean isUpdationDone =
          SecretsRestUtils.updateSecret(getAccount().getUuid(), bearerToken, secretsId, secretText);
      assertTrue(isUpdationDone);

      // Verifying the secret decryption
      List<EncryptedData> encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
      EncryptedData data = encryptedDataList.get(0);
      data.setEncryptionKey("SECRET_TEXT/" + secretText.getName());

      vaultConfigWithAppRoleSecret.setAuthToken(
          vaultService.appRoleLogin(vaultConfigWithAppRoleSecret).getClientToken());
      String decrypted =
          SecretsUtils.getValueFromName(secretManagementDelegateService, data, vaultConfigWithAppRoleSecret);
      assertEquals(secretText.getValue(), decrypted);

      boolean isDeletionDone = SecretsRestUtils.deleteSecret(getAccount().getUuid(), bearerToken, secretsId);
      assertTrue(isDeletionDone);
    } finally {
      VaultRestUtils.deleteVault(getAccount().getUuid(), bearerToken, appRoleVaultId);
      logger.info("AppRole based Vault secret manager deleted.");
    }
  }
}
