package io.harness.functional.secrets;

import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.security.encryption.EncryptionType;
import io.harness.testframework.framework.Setup;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.FeatureName;
import software.wings.service.intfc.FeatureFlagService;

import java.time.Instant;
import javax.ws.rs.core.GenericType;

@Slf4j
public class GcpSecretsManagerFunctionalTest extends AbstractFunctionalTest {
  private static final String GCP_KMS_NAME = "GCP KMS" + Instant.now().toEpochMilli();
  private static final String UPDATED_GCP_KMS_NAME = "Updated " + GCP_KMS_NAME;
  private static final String PROJECT_ID = "playground-243019";
  private static final String REGION = "us";
  private static final String KEY_RING = "kms-dev-test";
  private static final String KEY_NAME = "harness";
  private static final String REST_ENDPOINT_BASE = "/gcp-secrets-manager";

  @Inject private FeatureFlagService featureFlagService;

  private Account account;

  @Before
  public void setUp() {
    account = getAccount();
    enableFeatureFlag();
  }

  private void enableFeatureFlag() {
    featureFlagService.enableAccount(FeatureName.GOOGLE_KMS, account.getUuid());
    featureFlagService.enableAccount(FeatureName.GLOBAL_KMS_PRE_PROCESSING, account.getUuid());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  public void testGcpSecretsManagerCRUD() {
    String configId = saveGcpSecretsManager();
    assertThat(configId).isNotNull();
    String updatedConfigId = updateGcpSecretsManager(configId);
    assertThat(updatedConfigId).isEqualTo(configId);
    boolean isConfigDeleted = deleteGcpSecretsManager(configId);
    assertThat(isConfigDeleted).isTrue();
  }

  private String saveGcpSecretsManager() {
    RestResponse<String> restResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .multiPart("credentials", new ScmSecret().decryptToString(new SecretName("gcp_kms_service_account")))
            .config(RestAssured.config().encoderConfig(
                encoderConfig().encodeContentTypeAs("multipart/form-data", ContentType.JSON)))
            .queryParam("accountId", account.getUuid())
            .formParam("name", GCP_KMS_NAME)
            .formParam("encryptionType", EncryptionType.GCP_KMS)
            .formParam("projectId", PROJECT_ID)
            .formParam("region", REGION)
            .formParam("keyRing", KEY_RING)
            .formParam("keyName", KEY_NAME)
            .formParam("isDefault", false)
            .contentType("multipart/form-data")
            .post(REST_ENDPOINT_BASE)
            .as(new GenericType<RestResponse<String>>() {}.getType());
    return restResponse.getResource();
  }

  private String updateGcpSecretsManager(String configId) {
    RestResponse<String> restResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .multiPart("credentials", SECRET_MASK)
            .config(RestAssured.config().encoderConfig(
                encoderConfig().encodeContentTypeAs("multipart/form-data", ContentType.JSON)))
            .queryParam("accountId", account.getUuid())
            .formParam("name", UPDATED_GCP_KMS_NAME)
            .formParam("encryptionType", EncryptionType.GCP_KMS)
            .formParam("projectId", PROJECT_ID)
            .formParam("region", REGION)
            .formParam("keyRing", KEY_RING)
            .formParam("keyName", KEY_NAME)
            .formParam("isDefault", false)
            .contentType("multipart/form-data")
            .post(REST_ENDPOINT_BASE + "/" + configId)
            .as(new GenericType<RestResponse<String>>() {}.getType());
    return restResponse.getResource();
  }

  private Boolean deleteGcpSecretsManager(String configId) {
    RestResponse<Boolean> restResponse = Setup.portal()
                                             .auth()
                                             .oauth2(bearerToken)
                                             .queryParam("accountId", account.getUuid())
                                             .queryParam("configId", configId)
                                             .contentType("multipart/form-data")
                                             .delete(REST_ENDPOINT_BASE)
                                             .as(new GenericType<RestResponse<Boolean>>() {}.getType());
    return restResponse.getResource();
  }
}
