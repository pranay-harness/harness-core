/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.ApiKeyEntry;

import io.restassured.mapper.ObjectMapperType;
import javax.ws.rs.core.GenericType;

public class ApiKeysRestUtils {
  public static ApiKeyEntry createApiKey(String accountId, String bearerToken, ApiKeyEntry apiKeyEntry) {
    RestResponse<ApiKeyEntry> apiKeyEntryRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .body(apiKeyEntry, ObjectMapperType.GSON)
            .post("/api-keys")
            .as(new GenericType<RestResponse<ApiKeyEntry>>() {}.getType(), ObjectMapperType.GSON);
    return apiKeyEntryRestResponse.getResource();
  }

  public static ApiKeyEntry updateApiKey(String accountId, String bearerToken, ApiKeyEntry apiKeyEntry) {
    RestResponse<ApiKeyEntry> apiKeyEntryRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .body(apiKeyEntry, ObjectMapperType.GSON)
            .put("/api-keys/" + apiKeyEntry.getUuid())
            .as(new GenericType<RestResponse<ApiKeyEntry>>() {}.getType(), ObjectMapperType.GSON);
    return apiKeyEntryRestResponse.getResource();
  }

  public static ApiKeyEntry getApiKey(String accountId, String bearerToken, String apiKeyUUID) {
    RestResponse<ApiKeyEntry> apiKeyEntryRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .get("/api-keys/" + apiKeyUUID)
            .as(new GenericType<RestResponse<ApiKeyEntry>>() {}.getType(), ObjectMapperType.GSON);
    return apiKeyEntryRestResponse.getResource();
  }

  public static Integer deleteApiKey(String accountId, String bearerToken, String apiKeyUUID) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .delete("/api-keys/" + apiKeyUUID)
        .statusCode();
  }
}
