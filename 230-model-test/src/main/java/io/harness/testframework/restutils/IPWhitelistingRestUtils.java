/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testframework.restutils;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.security.access.Whitelist;

import io.restassured.mapper.ObjectMapperType;
import javax.ws.rs.core.GenericType;

public class IPWhitelistingRestUtils {
  public static Whitelist getWhitelistedIP(String accountId, String bearerToken, String whitelistId) {
    RestResponse<Whitelist> whitelistedIP =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .get("/whitelist/" + whitelistId)
            .as(new GenericType<RestResponse<Whitelist>>() {}.getType(), ObjectMapperType.GSON);
    return whitelistedIP.getResource();
  }

  public static Boolean deleteWhitelistedIP(String accountId, String bearerToken, String whitelistId) {
    RestResponse<Boolean> isDeleted =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .delete("/whitelist/" + whitelistId)
            .as(new GenericType<RestResponse<Boolean>>() {}.getType(), ObjectMapperType.GSON);
    return isDeleted.getResource();
  }

  public static Whitelist addWhiteListing(String accountId, String bearerToken, Whitelist whitelist) {
    RestResponse<Whitelist> whitelistedIP =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .body(whitelist, ObjectMapperType.GSON)
            .post("/whitelist")
            .as(new GenericType<RestResponse<Whitelist>>() {}.getType(), ObjectMapperType.GSON);

    return whitelistedIP.getResource();
  }

  public static Whitelist updateWhiteListing(String accountId, String bearerToken, Whitelist whitelist) {
    RestResponse<Whitelist> whitelistedIP =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .body(whitelist, ObjectMapperType.GSON)
            .put("/whitelist/" + whitelist.getUuid())
            .as(new GenericType<RestResponse<Whitelist>>() {}.getType(), ObjectMapperType.GSON);

    return whitelistedIP.getResource();
  }
}
