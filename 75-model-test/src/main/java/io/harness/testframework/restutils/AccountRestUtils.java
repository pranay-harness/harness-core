package io.harness.testframework.restutils;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import software.wings.beans.Account;

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
}
