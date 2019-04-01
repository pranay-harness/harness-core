package io.harness.functional.apikeys;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.framework.Setup;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.HarnessApiKey.ClientType;

import javax.ws.rs.core.GenericType;

/**
 * @author rktummala on 03/07/19
 */
public class HarnessApiKeyFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
  }

  @Test
  @Category(FunctionalTests.class)
  public void testCRUD() {
    deleteHarnessClientApiKey(ClientType.PROMETHEUS);
    deleteHarnessClientApiKey(ClientType.SALESFORCE);

    String createdKey = generateHarnessClientApiKey(ClientType.PROMETHEUS);
    assertThat(createdKey).isNotEmpty();

    String salesForceKey = generateHarnessClientApiKey(ClientType.SALESFORCE);
    assertThat(salesForceKey).isNotEqualTo(createdKey);

    String keyFromGet = getHarnessClientApiKey(ClientType.PROMETHEUS);
    assertThat(createdKey).isEqualTo(keyFromGet);

    deleteHarnessClientApiKey(ClientType.PROMETHEUS);

    keyFromGet = getHarnessClientApiKey(ClientType.PROMETHEUS);
    assertThat(keyFromGet).isNull();
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
