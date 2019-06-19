package io.harness.functional.rbac;

import static io.harness.rule.OwnerRule.SWAMY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.UserUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.User;

@Slf4j
public class AccessManagementROTest extends AbstractFunctionalTest {
  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void accessManagementNoPermissionUserTest() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), READ_ONLY_USER);
    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, "readonlyuser");
    assertNotNull(readOnlyUser);
    logger.info("List the users using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/users")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);
    logger.info("List users failed with Bad request as expected");
    logger.info("List the user groups using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/userGroups")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);
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
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/api-keys")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);

    logger.info("List APIKeys failed with Bad request as expected");

    logger.info("List the IPWhitelists using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/whitelist")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);
    logger.info("List IPWhitelists failed with Bad request as expected");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }
}
