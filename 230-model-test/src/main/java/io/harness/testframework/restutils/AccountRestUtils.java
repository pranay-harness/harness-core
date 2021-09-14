/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testframework.restutils;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.Account;
import software.wings.beans.LicenseUpdateInfo;

import io.restassured.http.ContentType;
import javax.ws.rs.core.GenericType;

public class AccountRestUtils {
  public static Account getAccount(String accountId, String bearerToken) {
    RestResponse<Account> accountRestResponse = Setup.portal()
                                                    .auth()
                                                    .oauth2(bearerToken)
                                                    .get("/account/" + accountId)
                                                    .as(new GenericType<RestResponse<Account>>() {}.getType());
    return accountRestResponse.getResource();
  }

  public static void updateAccountLicense(String accountId, String bearerToken, LicenseUpdateInfo licenseUpdateInfo) {
    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .body(licenseUpdateInfo)
        .contentType(ContentType.JSON)
        .put("/account/license")
        .as(new GenericType<RestResponse<Boolean>>() {}.getType());
  }
}
