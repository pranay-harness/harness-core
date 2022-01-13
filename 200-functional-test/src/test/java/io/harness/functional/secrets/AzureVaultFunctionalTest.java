/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.secrets;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.testframework.restutils.SecretsRestUtils.addSecret;
import static io.harness.testframework.restutils.SecretsRestUtils.deleteSecret;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

import io.harness.beans.EncryptedData;
import io.harness.beans.SecretText;
import io.harness.category.element.FunctionalTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryptors.clients.AzureVaultEncryptor;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.Setup;

import software.wings.beans.AzureVaultConfig;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import io.restassured.mapper.ObjectMapperType;
import java.util.List;
import javax.ws.rs.core.GenericType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author marklu on 9/11/19
 */
@Slf4j
public class AzureVaultFunctionalTest extends AbstractFunctionalTest {
  private static final String CLIENT_ID = "1ca0a31f-52e6-4766-b016-c6049c209040";
  private static final String TENANT_ID = "b229b2bb-5f33-4d22-bce0-730f6474e906";
  private static final String SUBSCRIPTION = "20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0";
  private static final String VAULT_NAME = "Aman-test";

  // private static final String SECRET_KEY = "9h.8Hfn64yHbe?i..lT_Gpg6Qq3?B86s";
  private static final String SECRET_KEY = new ScmSecret().decryptToString(new SecretName("qa_azure_vault_secret_key"));

  @Inject private AzureVaultEncryptor azureVaultEncryptor;
  @Inject private WingsPersistence wingsPersistence;

  private AzureVaultConfig azureVaultConfig;
  private String secretName;

  @Before
  public void setUp() {
    azureVaultConfig = getAzureVaultConfig();
    String secretsManagerId = saveSecretsManager(azureVaultConfig);
    assertNotNull(secretsManagerId);

    azureVaultConfig = wingsPersistence.get(AzureVaultConfig.class, secretsManagerId);
    // azureVaultConfig.setDefault(true);
    wingsPersistence.save(azureVaultConfig);

    List<AzureVaultConfig> secretManagerConfigs = listConfigs(getAccount().getUuid());
    assertTrue(secretManagerConfigs.size() > 0);

    azureVaultConfig.setSecretKey(SECRET_KEY);

    secretName = randomAlphanumeric(7);
  }

  @Test
  @Owner(developers = UTKARSH, intermittent = true)
  @Category(FunctionalTests.class)
  public void testCRUDSecrets() {
    String value = randomAlphanumeric(7);
    SecretText secretText =
        SecretText.builder().name(secretName).value(value).kmsId(azureVaultConfig.getUuid()).build();
    String secretId = addSecret(getAccount().getUuid(), bearerToken, secretText);

    EncryptedData data = wingsPersistence.get(EncryptedData.class, secretId);
    assertNotNull(data);

    char[] decrypted = azureVaultEncryptor.fetchSecretValue(getAccount().getUuid(), data, azureVaultConfig);
    assertTrue(isNotEmpty(decrypted));

    String decryptedSecret = String.valueOf(decrypted);
    assertEquals(value, decryptedSecret);

    boolean deleted = deleteSecret(getAccount().getUuid(), bearerToken, secretId);
    assertTrue(deleted);
  }

  @Test
  @Owner(developers = UTKARSH, intermittent = true)
  @Category(FunctionalTests.class)
  public void testCRUDSecretsReferenceWithoutVersion() {
    String secretPath = "DoNotChange-FunctionalTestSecret";
    SecretText secretText =
        SecretText.builder().name(secretName).kmsId(azureVaultConfig.getUuid()).path(secretPath).build();
    String secretId = addSecret(getAccount().getUuid(), bearerToken, secretText);

    EncryptedData data = wingsPersistence.get(EncryptedData.class, secretId);
    assertNotNull(data);

    char[] decrypted = azureVaultEncryptor.fetchSecretValue(getAccount().getUuid(), data, azureVaultConfig);
    assertTrue(isNotEmpty(decrypted));

    String decryptedSecret = String.valueOf(decrypted);
    assertEquals("Val2", decryptedSecret);

    boolean deleted = deleteSecret(getAccount().getUuid(), bearerToken, secretId);
    assertTrue(deleted);
  }

  @Test
  @Owner(developers = UTKARSH, intermittent = true)
  @Category(FunctionalTests.class)
  public void testCRUDSecretsReferenceWithVersion() {
    String secretPath = "DoNotChange-FunctionalTestSecret/128cf6016e0449c3ad02007c4881dd9a";
    SecretText secretText =
        SecretText.builder().name(secretName).path(secretPath).kmsId(azureVaultConfig.getUuid()).build();
    String secretId = addSecret(getAccount().getUuid(), bearerToken, secretText);

    EncryptedData data = wingsPersistence.get(EncryptedData.class, secretId);
    assertNotNull(data);

    char[] decrypted = azureVaultEncryptor.fetchSecretValue(getAccount().getUuid(), data, azureVaultConfig);
    assertTrue(isNotEmpty(decrypted));

    String decryptedSecret = String.valueOf(decrypted);
    assertEquals("Val1", decryptedSecret);

    boolean deleted = deleteSecret(getAccount().getUuid(), bearerToken, secretId);
    assertTrue(deleted);
  }

  @NotNull
  private AzureVaultConfig getAzureVaultConfig() {
    AzureVaultConfig azureVaultConfig = AzureVaultConfig.builder()
                                            .name(UUIDGenerator.generateUuid())
                                            .clientId(CLIENT_ID)
                                            .tenantId(TENANT_ID)
                                            .subscription(SUBSCRIPTION)
                                            .secretKey(SECRET_KEY)
                                            .vaultName(VAULT_NAME)
                                            .build();
    azureVaultConfig.setDefault(true);
    azureVaultConfig.setAccountId(getAccount().getUuid());
    return azureVaultConfig;
  }

  private String saveSecretsManager(AzureVaultConfig azureVaultConfig) {
    RestResponse<String> restResponse = Setup.portal()
                                            .auth()
                                            .oauth2(bearerToken)
                                            .queryParam("accountId", azureVaultConfig.getAccountId())
                                            .body(azureVaultConfig, ObjectMapperType.GSON)
                                            .post("/azure-secrets-manager")
                                            .as(new GenericType<RestResponse<String>>() {}.getType());
    return restResponse.getResource();
  }

  private static List<AzureVaultConfig> listConfigs(String accountId) {
    RestResponse<List<AzureVaultConfig>> secretsResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .get("/secrets/list-configs")
            .as(new GenericType<RestResponse<List<AzureVaultConfig>>>() {}.getType());
    return secretsResponse.getResource();
  }
}
