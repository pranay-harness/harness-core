/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
