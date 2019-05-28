package io.harness.e2e.dailysanity.platform;

import static junit.framework.TestCase.assertTrue;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.utils.SSOUtils;
import io.harness.testframework.restutils.SSORestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.OauthSettings;

@Slf4j
public class SSOTestPaid extends AbstractE2ETest {
  @Test()
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(E2ETests.class)
  public void createGoogleOauth() {
    logger.info("Starting the google oauth test");
    logger.info("Creating the oauth provider for google");
    OauthSettings oauthSettings = OauthSettings.builder().build();
    oauthSettings.setFilter("");
    oauthSettings.setDisplayName("google");
    assertTrue(SSORestUtils.addOauthSettings(getAccount().getUuid(), bearerToken, oauthSettings) == HttpStatus.SC_OK);
    Object ssoConfig = SSORestUtils.getAccessManagementSettings(getAccount().getUuid(), bearerToken);
    assertTrue(SSORestUtils.deleteOAUTHSettings(getAccount().getUuid(), bearerToken) == HttpStatus.SC_OK);
    logger.info("Done");
  }

  @Test()
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(E2ETests.class)
  public void createLDAPSettings() {
    logger.info("Starting the LDAP test");
    LdapSettings ldapSettings = SSOUtils.createDefaultLdapSettings(getAccount().getUuid());
    assertTrue(SSORestUtils.addLdapSettings(getAccount().getUuid(), bearerToken, ldapSettings) == HttpStatus.SC_OK);
    Object ssoConfig = SSORestUtils.getAccessManagementSettings(getAccount().getUuid(), bearerToken);
    assertTrue(SSORestUtils.deleteLDAPSettings(getAccount().getUuid(), bearerToken) == HttpStatus.SC_OK);
    logger.info("Done");
  }

  @Test()
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(E2ETests.class)
  public void createSAMLSettingsInPaid() {
    logger.info("Starting the LDAP test");
    String filePath = System.getProperty("user.dir");
    filePath = filePath + "/"
        + "src/test/resources/secrets/"
        + "SAML_SSO_Provider.xml";
    assertTrue(SSORestUtils.addSAMLSettings(getAccount().getUuid(), bearerToken, "SAML", filePath) == HttpStatus.SC_OK);
    Object ssoConfig = SSORestUtils.getAccessManagementSettings(getAccount().getUuid(), bearerToken);
    assertTrue(SSORestUtils.deleSAMLSettings(getAccount().getUuid(), bearerToken) == HttpStatus.SC_OK);
    logger.info("Done");
  }
}
