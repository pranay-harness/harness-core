/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.secrets;

import static io.harness.rule.OwnerRule.NATARAJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EncryptedData;
import io.harness.beans.SecretText;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.utils.SecretsUtils;
import io.harness.testframework.restutils.SecretsRestUtils;
import io.harness.testframework.restutils.VaultRestUtils;

import software.wings.beans.VaultConfig;
import software.wings.service.intfc.security.EncryptionService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class SecretsFunctionalTest extends AbstractFunctionalTest {
  @Inject private EncryptionService encryptionService;

  String vaultId = null;
  VaultConfig vaultConfig;
  final String VAULT_NAME = "Test Vault";
  final String QA_VAULT_URL = "https://vaultqa.harness.io";

  @Before
  public void vaultSetup() {
    vaultConfig = VaultConfig.builder()
                      .name(VAULT_NAME)
                      .vaultUrl(QA_VAULT_URL)
                      .authToken(new ScmSecret().decryptToString(new SecretName("qa_vault_root_token")))
                      .basePath("/harness")
                      .build();
    vaultConfig.setAccountId(getAccount().getUuid());
    vaultConfig.setDefault(true);

    List<VaultConfig> beforeVault = SecretsRestUtils.getListConfigs(getAccount().getUuid(), bearerToken);
    if (SecretsUtils.isVaultAvailable(beforeVault, VAULT_NAME)) {
      log.info("Vault already exists : " + VAULT_NAME);
      return;
    }

    vaultId = VaultRestUtils.addVault(bearerToken, vaultConfig);
    assertThat(StringUtils.isNotBlank(vaultId)).isTrue();
    log.info("Vault created : " + vaultId);
    List<VaultConfig> afterVault = SecretsRestUtils.getListConfigs(getAccount().getUuid(), bearerToken);
    if (SecretsUtils.isVaultAvailable(afterVault, VAULT_NAME)) {
      log.info("Vault existence verified : " + VAULT_NAME);
    }
    log.info("Done");
  }

  /*
   * This test is disabled as it uses delegate to setup vault from test, which is not working as of now.
   * Disbaling this temporarily until our team completes this from framework level.
   */
  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("This has some issue with datagen model, which may turn other tests flaky. Runs in E2ETest.")
  public void secretsCRUDTests() {
    log.info("Secrets test starts");
    String secretsName = "Secret-" + System.currentTimeMillis();
    String secretsNewName = "newName-" + System.currentTimeMillis();
    String secretValue = "value";

    SecretText secretText = SecretsUtils.createSecretTextObject(secretsName, secretValue);
    List<EncryptedData> encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
    boolean isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertThat(isSecretPresent).isFalse();

    String secretsId = SecretsRestUtils.addSecret(getAccount().getUuid(), bearerToken, secretText);
    assertThat(StringUtils.isNotBlank(secretsId)).isTrue();

    encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertThat(isSecretPresent).isTrue();
    secretText.setName(secretsNewName);

    boolean isUpdationDone = SecretsRestUtils.updateSecret(getAccount().getUuid(), bearerToken, secretsId, secretText);
    assertThat(isUpdationDone).isTrue();
    encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertThat(isSecretPresent).isTrue();

    // Verifying the secret decryption
    EncryptedData data = encryptedDataList.get(0);
    data.setEncryptionKey("SECRET_TEXT/" + secretsNewName);
    String decrypted = SecretsUtils.getValueFromName(encryptionService, data, vaultConfig);
    assertThat(decrypted).isEqualTo(secretValue);

    boolean isDeletionDone = SecretsRestUtils.deleteSecret(getAccount().getUuid(), bearerToken, secretsId);
    assertThat(isDeletionDone).isTrue();
    encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
    assertThat(encryptedDataList.size() == 0).isTrue();
  }

  @After
  public void vaultCleanup() {
    assertThat(VaultRestUtils.deleteVault(getAccount().getUuid(), bearerToken, vaultId)).isTrue();
    log.info("Vault Deleted. Test Clean up completed");
  }
}
