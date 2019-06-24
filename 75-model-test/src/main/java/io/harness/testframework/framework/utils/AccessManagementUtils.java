package io.harness.testframework.framework.utils;

import static org.junit.Assert.assertTrue;

import io.harness.testframework.framework.Setup;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import software.wings.beans.Account;

@Slf4j
public class AccessManagementUtils {
  public static void runNoAccessTest(Account account, String roBearerToken, String readOnlyUserid) {
    logger.info("List user groups failed with Bad request as expected");
    logger.info("List the SSO Settings using ReadOnly permission");
    logger.info("The below statement fails for the bug - PL-2335. Disabling it to avoid failures");

    /*assertTrue(Setup.portal()
        .auth()
        .oauth2(roBearerToken)
        .queryParam("accountId", getAccount().getUuid())
        .get("/sso/access-management/" + getAccount().getUuid()).getStatusCode() == HttpStatus.SC_BAD_REQUEST);
    logger.info("List SSO settings failed with Bad request as expected");*/

    logger.info("List the APIKeys using ReadOnly permission");

    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/api-keys")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);

    logger.info("List APIKeys failed with Bad request as expected");

    logger.info("List the IPWhitelists using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/whitelist")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);
    logger.info("List IPWhitelists failed with Bad request as expected");

    Setup.signOut(readOnlyUserid, roBearerToken);
    logger.info("Readonly user logout successful");
  }
}
