package io.harness.functional.secrets;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.restutils.SecretsRestUtils;
import io.restassured.mapper.ObjectMapperType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.AzureVaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import java.util.List;
import javax.ws.rs.core.GenericType;

/**
 * @author marklu on 9/11/19
 */
@Slf4j
public class AzureVaultFunctionalTest extends AbstractFunctionalTest {
  private static final String SM_NAME = "Azure Key Vault";
  private static final String CLIENT_ID = "19875049-d2a5-47c3-b1fe-f8babd7caf30";
  private static final String TENANT_ID = "b229b2bb-5f33-4d22-bce0-730f6474e906";
  private static final String SUBSRIPTION = "20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0";
  private static final String VAULT_NAME = "Aman-test";

  // private static final String SECRET_KEY = "[9qoJskuCWp1/?:l0XZH36V:OXepB-=q";
  private static final String SECRET_KEY = new ScmSecret().decryptToString(new SecretName("qa_azure_vault_secret_key"));

  @Inject private SecretManagementDelegateService secretManagementDelegateService;
  @Inject private WingsPersistence wingsPersistence;

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  @Ignore("Marked as ignore as this test is flaky and failing intermittently")
  public void testCRUDSecretsWithAzureVaultSecretManager() throws Exception {
    AzureVaultConfig azureVaultConfig = AzureVaultConfig.builder()
                                            .name(SM_NAME)
                                            .clientId(CLIENT_ID)
                                            .tenantId(TENANT_ID)
                                            .subscription(SUBSRIPTION)
                                            .secretKey(SECRET_KEY)
                                            .vaultName(VAULT_NAME)
                                            .build();
    azureVaultConfig.setDefault(true);
    azureVaultConfig.setAccountId(getAccount().getUuid());

    String secretsManagerId = null;
    String secretId = null;
    try {
      secretsManagerId = saveSecretsManager(azureVaultConfig);
      assertThat(secretsManagerId).isNotNull();
      logger.info("Azure Key Vault Secret Manager config created.");

      azureVaultConfig = wingsPersistence.get(AzureVaultConfig.class, secretsManagerId);
      azureVaultConfig.setDefault(true);
      wingsPersistence.save(azureVaultConfig);

      List<AzureVaultConfig> secretManagerConfigs = listConfigs(getAccount().getUuid());
      assertThat(secretManagerConfigs.size() > 0).isTrue();

      String secretName = "MyCyberArkSecret";
      String value = "MySecretValue";
      SecretText secretText = SecretText.builder().name(secretName).value(value).build();

      secretId = SecretsRestUtils.addSecret(getAccount().getUuid(), bearerToken, secretText);
      assertThat(StringUtils.isNotBlank(secretId)).isTrue();

      List<EncryptedData> encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
      assertThat(encryptedDataList.size() > 0).isTrue();

      EncryptedData data = wingsPersistence.get(EncryptedData.class, secretId);
      assertThat(data).isNotNull();

      // Verifying the secret decryption
      azureVaultConfig.setSecretKey(SECRET_KEY);
      char[] decrypted = secretManagementDelegateService.decrypt(data, azureVaultConfig);
      assertThat(EmptyPredicate.isNotEmpty(decrypted)).isTrue();
      String decryptedSecret = String.valueOf(decrypted);
      logger.info("Decrypted value: {}", decryptedSecret);
      assertThat(decryptedSecret).isEqualTo(value);
    } finally {
      if (secretId != null) {
        boolean deleted = SecretsRestUtils.deleteSecret(getAccount().getUuid(), bearerToken, secretId);
        assertThat(deleted).isTrue();
        logger.info("Secret with id {} has been deleted: {}", secretId, deleted);
      }
      if (secretsManagerId != null) {
        deleteSecretsManager(getAccount().getUuid(), secretsManagerId);
        logger.info("CyberArk Secrets Manager deleted.");
      }
    }
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

  private Boolean deleteSecretsManager(String accountId, String secretsManagerConfigId) {
    RestResponse<Boolean> restResponse = Setup.portal()
                                             .auth()
                                             .oauth2(bearerToken)
                                             .queryParam("accountId", accountId)
                                             .queryParam("configId", secretsManagerConfigId)
                                             .delete("/azure-secrets-manager")
                                             .as(new GenericType<RestResponse<Boolean>>() {}.getType());
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
