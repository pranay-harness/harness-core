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
import io.restassured.mapper.ObjectMapperType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.GlobalApiKey.ProviderType;

import javax.ws.rs.core.GenericType;

/**
 * @author rktummala on 03/07/19
 */
public class GlobalApiKeyFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
  }

  @Test
  @Category(FunctionalTests.class)
  public void testCRUD() {
    deleteGlobalApiKey(ProviderType.PROMETHEUS);
    deleteGlobalApiKey(ProviderType.SALESFORCE);

    String createdKey = generateGlobalApiKey(ProviderType.PROMETHEUS);
    assertThat(createdKey).isNotEmpty();

    String salesForceKey = generateGlobalApiKey(ProviderType.SALESFORCE);
    assertThat(salesForceKey).isNotEqualTo(createdKey);

    String keyFromGet = getGlobalApiKey(ProviderType.PROMETHEUS);
    assertThat(createdKey).isEqualTo(keyFromGet);

    deleteGlobalApiKey(ProviderType.PROMETHEUS);

    keyFromGet = getGlobalApiKey(ProviderType.PROMETHEUS);
    assertThat(keyFromGet).isNull();
  }

  private String generateGlobalApiKey(ProviderType providerType) {
    GenericType<RestResponse<String>> returnType = new GenericType<RestResponse<String>>() {};
    RestResponse<String> response = Setup.portal()
                                        .auth()
                                        .oauth2(bearerToken)
                                        .body(providerType, ObjectMapperType.GSON)
                                        .contentType(ContentType.JSON)
                                        .post("/global-api-keys")
                                        .as(returnType.getType());
    return response.getResource();
  }

  private String getGlobalApiKey(ProviderType providerType) {
    GenericType<RestResponse<String>> returnType = new GenericType<RestResponse<String>>() {};
    RestResponse<String> response = Setup.portal()
                                        .auth()
                                        .oauth2(bearerToken)
                                        .contentType(ContentType.JSON)
                                        .get("/global-api-keys/" + providerType)
                                        .as(returnType.getType());
    return response.getResource();
  }

  private RestResponse<Void> deleteGlobalApiKey(ProviderType providerType) {
    GenericType<RestResponse<Void>> returnType = new GenericType<RestResponse<Void>>() {};
    RestResponse<Void> response = Setup.portal()
                                      .auth()
                                      .oauth2(bearerToken)
                                      .contentType(ContentType.JSON)
                                      .delete("/global-api-keys/" + providerType)
                                      .as(returnType.getType());
    return response;
  }
}
