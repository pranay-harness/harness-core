package io.harness.functional.secrets;

import static io.harness.rule.OwnerRule.MARK;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.restutils.SecretsRestUtils;
import io.restassured.mapper.ObjectMapperType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import java.io.InputStream;
import java.util.List;
import javax.ws.rs.core.GenericType;

/**
 * @author marklu on 2019-08-01
 */
@Slf4j
public class CyberArkServiceFunctionalTest extends AbstractFunctionalTest {
  private static final String SM_NAME = "CyberArk Secrets Manager";
  private static final String APP_ID = "testappid";

  @Inject private SecretManagementDelegateService secretManagementDelegateService;
  @Inject private WingsPersistence wingsPersistence;

  @Test
  @Owner(emails = MARK)
  @Category(FunctionalTests.class)
  @Ignore("This test is depending on a ephemeral CyberArk installation in skytap.com which need manually brought up")
  public void testCRUDSecretsWithCyberArkSecretManager() throws Exception {
    InputStream inputStream = CyberArkServiceFunctionalTest.class.getResourceAsStream("/certs/clientCert.pem");
    String clientCertificate = IOUtils.toString(inputStream, "UTF-8");

    CyberArkConfig cyberArkConfig = CyberArkConfig.builder()
                                        .name(SM_NAME)
                                        .appId(APP_ID)
                                        .cyberArkUrl("https://services-uscentral.skytap.com:17139")
                                        .clientCertificate(clientCertificate)
                                        .build();
    cyberArkConfig.getEncryptionType();
    cyberArkConfig.setDefault(true);
    cyberArkConfig.setAccountId(getAccount().getUuid());

    String secretsManagerId = null;
    String secretId = null;
    try {
      secretsManagerId = addSecretsManager(cyberArkConfig);
      assertNotNull(secretsManagerId);
      logger.info("CyberArk Secret Manager config created.");

      cyberArkConfig = wingsPersistence.get(CyberArkConfig.class, secretsManagerId);
      cyberArkConfig.setDefault(true);
      wingsPersistence.save(cyberArkConfig);

      List<CyberArkConfig> cyberArkConfigs = listConfigs(getAccount().getUuid());
      assertTrue(cyberArkConfigs.size() > 0);

      String secretName = "MyCyberArkSecret";
      // String query = "Safe=Test;Folder=root\\OS\\Windows;Object=windows1";
      String query = "Address=components;Username=svc_account";
      SecretText secretText = SecretText.builder().name(secretName).path(query).build();

      secretId = SecretsRestUtils.addSecret(getAccount().getUuid(), bearerToken, secretText);
      assertTrue(StringUtils.isNotBlank(secretId));

      List<EncryptedData> encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
      assertTrue(encryptedDataList.size() > 0);

      EncryptedData data = wingsPersistence.get(EncryptedData.class, secretId);
      assertNotNull(data);

      // Verifying the secret decryption
      cyberArkConfig.setClientCertificate(clientCertificate);
      char[] decrypted = secretManagementDelegateService.decrypt(data, cyberArkConfig);
      assertTrue(EmptyPredicate.isNotEmpty(decrypted));
      String decryptedSecret = String.valueOf(decrypted);
      logger.info("Decrypted value: {}", decryptedSecret);
      assertEquals(":m23LF6f", decryptedSecret);
    } finally {
      if (secretId != null) {
        boolean deleted = SecretsRestUtils.deleteSecret(getAccount().getUuid(), bearerToken, secretId);
        assertTrue(deleted);
        logger.info("Secret with id {} has been deleted: {}", secretId, deleted);
      }
      if (secretsManagerId != null) {
        deleteSecretsManager(getAccount().getUuid(), secretsManagerId);
        logger.info("CyberArk Secrets Manager deleted.");
      }
    }
  }

  private String addSecretsManager(CyberArkConfig cyberArkConfig) {
    RestResponse<String> restResponse = Setup.portal()
                                            .auth()
                                            .oauth2(bearerToken)
                                            .queryParam("accountId", cyberArkConfig.getAccountId())
                                            .body(cyberArkConfig, ObjectMapperType.GSON)
                                            .post("/cyberark")
                                            .as(new GenericType<RestResponse<String>>() {}.getType());
    return restResponse.getResource();
  }

  private Boolean deleteSecretsManager(String accountId, String secretsManagerConfigId) {
    RestResponse<Boolean> restResponse = Setup.portal()
                                             .auth()
                                             .oauth2(bearerToken)
                                             .queryParam("accountId", accountId)
                                             .queryParam("configId", secretsManagerConfigId)
                                             .delete("/cyberark")
                                             .as(new GenericType<RestResponse<Boolean>>() {}.getType());
    return restResponse.getResource();
  }

  private static List<CyberArkConfig> listConfigs(String accountId) {
    RestResponse<List<CyberArkConfig>> secretsResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .get("/secrets/list-values")
            .as(new GenericType<RestResponse<List<AwsSecretsManagerConfig>>>() {}.getType());
    return secretsResponse.getResource();
  }
}
