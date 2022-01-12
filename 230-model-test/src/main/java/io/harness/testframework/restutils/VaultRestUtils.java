/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.VaultConfig;

import io.restassured.mapper.ObjectMapperType;
import javax.ws.rs.core.GenericType;
import org.apache.commons.lang3.StringUtils;

public class VaultRestUtils {
  public static String addVault(String bearerToken, VaultConfig vaultConfig) {
    RestResponse<String> vaultRestResponse = Setup.portal()
                                                 .auth()
                                                 .oauth2(bearerToken)
                                                 .queryParam("accountId", vaultConfig.getAccountId())
                                                 .body(vaultConfig, ObjectMapperType.GSON)
                                                 .post("/vault")
                                                 .as(new GenericType<RestResponse<String>>() {}.getType());
    return vaultRestResponse.getResource();
  }

  public static boolean deleteVault(String accountId, String bearerToken, String vaultConfigId) {
    if (StringUtils.isBlank(vaultConfigId)) {
      return true;
    }
    RestResponse<Boolean> vaultRestResponse = Setup.portal()
                                                  .auth()
                                                  .oauth2(bearerToken)
                                                  .queryParam("accountId", accountId)
                                                  .queryParam("vaultConfigId", vaultConfigId)
                                                  .delete("/vault")
                                                  .as(new GenericType<RestResponse<Boolean>>() {}.getType());
    return vaultRestResponse.getResource();
  }
}
