package io.harness.functional.secrets;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.SecretsUtils;
import io.harness.testframework.restutils.SecretsRestUtils;
import io.restassured.mapper.ObjectMapperType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.AwsSecretsManagerService;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import java.util.List;
import javax.ws.rs.core.GenericType;

/**
 * @author marklu on 2019-05-08
 */
@Slf4j
public class AwsSecretsManagerServiceFunctionalTest extends AbstractFunctionalTest {
  private static final String ASM_NAME = "AWS Secrets Manager";

  @Inject private SecretManagementDelegateService secretManagementDelegateService;
  @Inject private AwsSecretsManagerService secretsManagerService;

  @Test
  @Category(FunctionalTests.class)
  @Ignore
  public void testCRUDSecretsWithAwsSecretsManager() {
    AwsSecretsManagerConfig secretsManagerConfig = AwsSecretsManagerConfig.builder()
                                                       .accountId(getAccount().getUuid())
                                                       .name(ASM_NAME)
                                                       .accessKey("AKIA5GUB5GGCMWOWNBEV")
                                                       .secretKey("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")
                                                       .region("us-east-2")
                                                       .secretNamePrefix("foo/bar")
                                                       .isDefault(true)
                                                       .build();

    String secretsManagerId = addAwsSecretsManager(secretsManagerConfig);
    assertNotNull(secretsManagerId);
    logger.info("AWS Secrets Manager config created.");

    try {
      List<AwsSecretsManagerConfig> secretsManagerConfigs = listConfigs(getAccount().getUuid());
      assertTrue(secretsManagerConfigs.size() > 0);

      String secretName = "MySecret";
      String secretValue = "MyValue";
      SecretText secretText = SecretsUtils.createSecretTextObject(secretName, secretValue);

      String secretId = SecretsRestUtils.addSecret(getAccount().getUuid(), bearerToken, secretText);
      assertTrue(StringUtils.isNotBlank(secretId));

      secretName = "MySecret-Updated";
      secretValue = "MyValue-Updated";
      secretText.setName(secretName);
      secretText.setValue(secretValue);
      boolean isUpdationDone = SecretsRestUtils.updateSecret(getAccount().getUuid(), bearerToken, secretId, secretText);
      assertTrue(isUpdationDone);

      // Verifying the secret decryption
      List<EncryptedData> encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
      assertTrue(encryptedDataList.size() > 0);

      boolean isDeletionDone = SecretsRestUtils.deleteSecret(getAccount().getUuid(), bearerToken, secretId);
      assertTrue(isDeletionDone);
    } finally {
      deleteAwsSecretsManager(getAccount().getUuid(), secretsManagerId);
      logger.info("AWS Secrets Manager deleted.");
    }
  }

  private String addAwsSecretsManager(AwsSecretsManagerConfig secretsManagerConfig) {
    RestResponse<String> restResponse = Setup.portal()
                                            .auth()
                                            .oauth2(bearerToken)
                                            .queryParam("accountId", secretsManagerConfig.getAccountId())
                                            .body(secretsManagerConfig, ObjectMapperType.GSON)
                                            .post("/aws-secrets-manager")
                                            .as(new GenericType<RestResponse<String>>() {}.getType());
    return restResponse.getResource();
  }

  private Boolean deleteAwsSecretsManager(String accountId, String secretsManagerConfigId) {
    RestResponse<Boolean> restResponse = Setup.portal()
                                             .auth()
                                             .oauth2(bearerToken)
                                             .queryParam("accountId", accountId)
                                             .queryParam("configId", secretsManagerConfigId)
                                             .delete("/aws-secrets-manager")
                                             .as(new GenericType<RestResponse<Boolean>>() {}.getType());
    return restResponse.getResource();
  }

  private static List<AwsSecretsManagerConfig> listConfigs(String accountId) {
    RestResponse<List<AwsSecretsManagerConfig>> secretsResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .get("/secrets/list-values")
            .as(new GenericType<RestResponse<List<AwsSecretsManagerConfig>>>() {}.getType());
    return secretsResponse.getResource();
  }
}
