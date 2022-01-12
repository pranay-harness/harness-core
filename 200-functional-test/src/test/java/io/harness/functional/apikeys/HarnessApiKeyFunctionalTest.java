/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.apikeys;

import static io.harness.rule.OwnerRule.RAMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.ClientType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;

import software.wings.security.SecretManager;

import com.google.inject.Inject;
import io.restassured.http.ContentType;
import javax.ws.rs.core.GenericType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author rktummala on 03/07/19
 */
@Slf4j
public class HarnessApiKeyFunctionalTest extends AbstractFunctionalTest {
  @Inject private SecretManager secretManager;
  @Inject private OwnerManager ownerManager;
  private Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
  }

  @Test
  @Owner(developers = RAMA)
  @Category(FunctionalTests.class)
  public void testCRUD() {
    String createdKey = generateHarnessClientApiKey(ClientType.PROMETHEUS);
    assertThat(createdKey).isNotEmpty();

    String salesForceKey = generateHarnessClientApiKey(ClientType.SALESFORCE);
    assertThat(salesForceKey).isNotEqualTo(createdKey);

    String internalKey = generateHarnessClientApiKey(ClientType.INTERNAL);
    assertThat(internalKey).isNotEqualTo(createdKey);

    String identityServiceKey = getHarnessClientApiKey(ClientType.IDENTITY_SERVICE);
    assertThat(createdKey).isNotEqualTo(identityServiceKey);

    String keyFromGet = getHarnessClientApiKey(ClientType.PROMETHEUS);
    assertThat(createdKey).isEqualTo(keyFromGet);

    deleteHarnessClientApiKey(ClientType.PROMETHEUS);
    deleteHarnessClientApiKey(ClientType.SALESFORCE);
    deleteHarnessClientApiKey(ClientType.INTERNAL);
    deleteHarnessClientApiKey(ClientType.IDENTITY_SERVICE);

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
