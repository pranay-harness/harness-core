package io.harness.functional.sso;

import static io.harness.rule.OwnerRule.SWAMY;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.utils.SSOUtils;
import io.harness.testframework.restutils.SSORestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.sso.LdapSettings;

@Slf4j
public class SSOCRUDTest extends AbstractFunctionalTest {
  @Test()
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void testLDAPCRUD() {
    logger.info("Starting the LDAP test");
    logger.info("Creating LDAP SSO Setting");
    LdapSettings ldapSettings = SSOUtils.createDefaultLdapSettings(getAccount().getUuid());
    assertTrue(SSORestUtils.addLdapSettings(getAccount().getUuid(), bearerToken, ldapSettings) == HttpStatus.SC_OK);
    Object ssoConfig = SSORestUtils.getAccessManagementSettings(getAccount().getUuid(), bearerToken);
    assertNotNull(ssoConfig);
    String ldapId = SSOUtils.getLdapId(ssoConfig);
    assertTrue(StringUtils.isNotBlank(ldapId));
    assertTrue(SSORestUtils.deleteLDAPSettings(getAccount().getUuid(), bearerToken) == HttpStatus.SC_OK);
    logger.info("LDAP CRUD test completed");
  }

  @Test()
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void testSAMLCRUD() {
    logger.info("Starting the SAML test");
    String filePath = System.getProperty("user.dir");
    filePath = filePath + "/"
        + "src/test/resources/secrets/"
        + "SAML_SSO_Provider.xml";
    assertTrue(SSORestUtils.addSAMLSettings(getAccount().getUuid(), bearerToken, "SAML", filePath) == HttpStatus.SC_OK);
    Object ssoConfig = SSORestUtils.getAccessManagementSettings(getAccount().getUuid(), bearerToken);
    assertNotNull(ssoConfig);
    assertTrue(SSORestUtils.deleSAMLSettings(getAccount().getUuid(), bearerToken) == HttpStatus.SC_OK);
    logger.info("Done");
  }
}
