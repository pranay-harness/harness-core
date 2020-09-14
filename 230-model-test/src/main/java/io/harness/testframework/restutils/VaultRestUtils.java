package io.harness.testframework.restutils;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.restassured.mapper.ObjectMapperType;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.VaultConfig;

import javax.ws.rs.core.GenericType;

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
