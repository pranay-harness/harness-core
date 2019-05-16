package software.wings.licensing.violations.checkers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureUsageViolation;
import software.wings.beans.FeatureViolation;
import software.wings.beans.sso.LdapConnectionSettings;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.OauthSettings;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOType;
import software.wings.beans.sso.SamlSettings;
import software.wings.service.intfc.SSOSettingService;

import java.util.List;

public class SSOViolationCheckerTest extends WingsBaseTest {
  public static final String TEST_SAML_SSO = "TEST SAML";
  public static final String TEST_LDAP_SSO = "TEST LDAP";
  public static final String TEST_OAUTH_SSO = "TEST OAUTH";
  public static final String TEST_ACCOUNT_ID = "ACCOUNT_ID";

  @Mock private SSOSettingService ssoSettingService;

  @InjectMocks @Inject private SSOViolationChecker ssoViolationChecker;

  @Test
  @Category(UnitTests.class)
  public void testValidSSOViolations() {
    List<SSOSettings> ssoSettingsList = getRestrictedSSOSettings();
    when(ssoSettingService.getAllSsoSettings(Mockito.any(String.class))).thenReturn(ssoSettingsList);
    List<FeatureViolation> featureViolationList = ssoViolationChecker.check(TEST_ACCOUNT_ID, AccountType.COMMUNITY);
    assertNotNull(featureViolationList);
    assertEquals(1, featureViolationList.size());
    assertEquals(((FeatureUsageViolation) featureViolationList.get(0)).getUsageCount(), ssoSettingsList.size());
    assertTrue("SSOType should be either LDAP OR SAML",
        ((FeatureUsageViolation) featureViolationList.get(0))
            .getUsages()
            .stream()
            .allMatch(usage
                -> (SSOType.SAML.name().equals(usage.getEntityType()) && TEST_SAML_SSO.equals(usage.getEntityName()))
                    || (SSOType.LDAP.name().equals(usage.getEntityType())
                           && TEST_LDAP_SSO.equals(usage.getEntityName()))));
  }

  @Test
  @Category(UnitTests.class)
  public void testInvalidSSOViolations() {
    List<SSOSettings> ssoSettingsList = getAllowedSSOSettings();
    when(ssoSettingService.getAllSsoSettings(Mockito.any(String.class))).thenReturn(ssoSettingsList);
    List<FeatureViolation> featureViolationList = ssoViolationChecker.check(TEST_ACCOUNT_ID, AccountType.COMMUNITY);
    assertNotNull(featureViolationList);
    assertTrue(featureViolationList.isEmpty());
  }

  private List<SSOSettings> getRestrictedSSOSettings() {
    SamlSettings samlSettings = SamlSettings.builder().displayName(TEST_SAML_SSO).ssoType(SSOType.SAML).build();
    samlSettings.setUuid(generateUuid());

    LdapSettings ldapSettings =
        LdapSettings.builder().displayName(TEST_LDAP_SSO).connectionSettings(new LdapConnectionSettings()).build();
    ldapSettings.setUuid(generateUuid());
    ldapSettings.setType(SSOType.LDAP);

    return Lists.newArrayList(samlSettings, ldapSettings);
  }

  private List<SSOSettings> getAllowedSSOSettings() {
    OauthSettings oauthSettings = OauthSettings.builder().displayName(TEST_OAUTH_SSO).build();
    oauthSettings.setType(SSOType.OAUTH);
    oauthSettings.setUuid(generateUuid());
    return Lists.newArrayList(oauthSettings);
  }
}
