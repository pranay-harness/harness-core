package io.harness.functional.apikeys;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.HarnessApiKey.ClientType;
import software.wings.beans.User;
import software.wings.security.SecretManager;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.service.intfc.HarnessApiKeyService;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;

/**
 * @author rktummala on 03/07/19
 */
@Slf4j
public class HarnessApiKeyFunctionalTest extends AbstractFunctionalTest {
  @Inject private SecretManager secretManager;
  @Inject private OwnerManager ownerManager;
  Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
  }

  @Test
  @Category(FunctionalTests.class)
  public void testCRUD() {
    String createdKey = generateHarnessClientApiKey(ClientType.PROMETHEUS);
    assertThat(createdKey).isNotEmpty();

    String salesForceKey = generateHarnessClientApiKey(ClientType.SALESFORCE);
    assertThat(salesForceKey).isNotEqualTo(createdKey);

    String internalKey = generateHarnessClientApiKey(ClientType.INTERNAL);
    assertThat(internalKey).isNotEqualTo(createdKey);

    String keyFromGet = getHarnessClientApiKey(ClientType.PROMETHEUS);
    assertThat(createdKey).isEqualTo(keyFromGet);

    deleteHarnessClientApiKey(ClientType.PROMETHEUS);
    deleteHarnessClientApiKey(ClientType.SALESFORCE);
    deleteHarnessClientApiKey(ClientType.INTERNAL);

    keyFromGet = getHarnessClientApiKey(ClientType.PROMETHEUS);
    assertThat(keyFromGet).isNull();
  }

  @Test
  @Category(FunctionalTests.class)
  public void testIdentityServiceClientWithApiKey() {
    try {
      String identityServiceApiKey = generateHarnessClientApiKey(ClientType.IDENTITY_SERVICE);
      logger.info("IDENTITY_SERVICE Api Key: {}", identityServiceApiKey);
      Map<String, String> claims = new HashMap<>();
      claims.put("env", "gateway");
      String apiKeyToken = secretManager.generateJWTToken(claims, identityServiceApiKey, JWT_CATEGORY.API_KEY);
      logger.info("IDENTITY_SERVICE Api Key Token: {}", apiKeyToken);
      User user = loginUserForIdentityService(apiKeyToken);
      assertThat(user).isNotNull();
      assertThat(user.getEmail()).isEqualTo(ADMIN_USER);
      assertThat(user.getToken()).isNullOrEmpty();
    } finally {
      deleteHarnessClientApiKey(ClientType.IDENTITY_SERVICE);
    }
  }

  private User loginUserForIdentityService(String apiKeyToken) {
    GenericType<RestResponse<User>> returnType = new GenericType<RestResponse<User>>() {};
    RestResponse<User> response =
        Setup.portal()
            .header(HttpHeaders.AUTHORIZATION, HarnessApiKeyService.PREFIX_API_KEY_TOKEN + " " + apiKeyToken)
            .contentType(ContentType.JSON)
            .get("identity/user/login?email=" + ADMIN_USER)
            .as(returnType.getType());
    return response.getResource();
  }

  private String generateHarnessClientApiKey(ClientType clientType) {
    GenericType<RestResponse<String>> returnType = new GenericType<RestResponse<String>>() {};
    RestResponse<String> response = Setup.portal()
                                        .auth()
                                        .oauth2(bearerToken)
                                        .body(clientType.name())
                                        .contentType(ContentType.JSON)
                                        .post("harness-api-keys")
                                        .as(returnType.getType());
    return response.getResource();
  }

  private String getHarnessClientApiKey(ClientType clientType) {
    GenericType<RestResponse<String>> returnType = new GenericType<RestResponse<String>>() {};
    RestResponse<String> response = Setup.portal()
                                        .auth()
                                        .oauth2(bearerToken)
                                        .contentType(ContentType.JSON)
                                        .get("harness-api-keys/" + clientType.name())
                                        .as(returnType.getType());
    return response.getResource();
  }

  private RestResponse<Boolean> deleteHarnessClientApiKey(ClientType clientType) {
    GenericType<RestResponse<Boolean>> returnType = new GenericType<RestResponse<Boolean>>() {};
    RestResponse<Boolean> response = Setup.portal()
                                         .auth()
                                         .oauth2(bearerToken)
                                         .contentType(ContentType.JSON)
                                         .delete("harness-api-keys/" + clientType.name())
                                         .as(returnType.getType());
    return response;
  }
}
