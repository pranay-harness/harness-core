package software.wings.service;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.RUSHABH;
import static io.harness.rule.OwnerRule.UJJAWAL;

import static software.wings.security.authentication.AuthenticationMechanism.USER_PASSWORD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Event;
import software.wings.beans.sso.SamlSettings;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserGroupService;

import com.google.inject.Inject;
import javax.validation.ConstraintViolationException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class SSOSettingServiceTest extends WingsBaseTest {
  @Mock UserGroupService userGroupService;
  @Mock AuditServiceHelper auditServiceHelper;
  @Inject AccountService accountService;
  @Inject @InjectMocks SSOSettingService ssoSettingService;

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testSamlSettingsSaveAuditing() {
    Account account = Account.Builder.anAccount()
                          .withUuid("TestAccountID")
                          .withOauthEnabled(false)
                          .withAccountName("Account_1")
                          .withLicenseInfo(getLicenseInfo())
                          .withAppId("appId")
                          .withCompanyName("Account_2")
                          .withAuthenticationMechanism(USER_PASSWORD)
                          .build();
    accountService.save(account, false);
    SamlSettings samlSettings = SamlSettings.builder()
                                    .metaDataFile("TestMetaDataFile")
                                    .url("TestURL")
                                    .accountId("TestAccountID")
                                    .displayName("Okta")
                                    .origin("TestOrigin")
                                    .build();

    samlSettings = ssoSettingService.saveSamlSettings(samlSettings);
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin");
    verify(auditServiceHelper, times(1))
        .reportForAuditingUsingAccountId(
            eq(samlSettings.getAccountId()), eq(null), any(SamlSettings.class), eq(Event.Type.CREATE));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testSamlSettingsCRUD() {
    Account account = Account.Builder.anAccount()
                          .withUuid("TestAccountID")
                          .withOauthEnabled(false)
                          .withAccountName("Account 1")
                          .withLicenseInfo(getLicenseInfo())
                          .withAppId("app_id")
                          .withCompanyName("Account 2")
                          .withAuthenticationMechanism(USER_PASSWORD)
                          .build();
    accountService.save(account, false);

    SamlSettings samlSettings = SamlSettings.builder()
                                    .metaDataFile("TestMetaDataFile")
                                    .url("TestURL")
                                    .accountId("TestAccountID")
                                    .displayName("Okta")
                                    .origin("TestOrigin")
                                    .build();

    samlSettings = ssoSettingService.saveSamlSettings(samlSettings);
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin");

    samlSettings = ssoSettingService.getSamlSettingsByIdpUrl("TestURL");
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin");

    samlSettings = ssoSettingService.getSamlSettingsByAccountId("TestAccountID");
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin");

    samlSettings = ssoSettingService.getSamlSettingsByOrigin("TestOrigin");
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin");

    samlSettings = SamlSettings.builder()
                       .metaDataFile("TestMetaDataFile2")
                       .url("TestURL2")
                       .accountId("TestAccountID")
                       .displayName("Okta")
                       .origin("TestOrigin2")
                       .build();

    samlSettings = ssoSettingService.saveSamlSettings(samlSettings);
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL2");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile2");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin2");

    samlSettings = ssoSettingService.getSamlSettingsByIdpUrl("TestURL2");
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL2");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile2");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin2");

    samlSettings = ssoSettingService.getSamlSettingsByAccountId("TestAccountID");
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL2");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile2");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin2");

    samlSettings = ssoSettingService.getSamlSettingsByOrigin("TestOrigin2");
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL2");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile2");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin2");

    assertThat(ssoSettingService.getSamlSettingsByAccountId("TestAccountID3")).isNull();

    assertThat(ssoSettingService.getSamlSettingsByIdpUrl("FakeURL")).isNull();
    assertThat(ssoSettingService.getSamlSettingsByOrigin("FakeOrigin")).isNull();

    // Deletion would not be allowed because there is no saml setting with account `TestAccountID3`
    assertThatThrownBy(() -> ssoSettingService.deleteSamlSettings("TestAccountID3"))
        .isInstanceOf(InvalidRequestException.class);

    // Mocking the userGroupService to return true when existsLinkedUserGroup is checked.
    when(userGroupService.existsLinkedUserGroup(samlSettings.getUuid())).thenReturn(true);

    // Because there is a linked user group with this SsoId, the deleteSamlSetting should not succeed.
    assertThatThrownBy(() -> ssoSettingService.deleteSamlSettings("TestAccountID"))
        .isInstanceOf(InvalidRequestException.class);

    // Mocking the userGroupService to return false when existsLinkedUserGroup is checked.
    when(userGroupService.existsLinkedUserGroup(samlSettings.getUuid())).thenReturn(false);

    assertThat(ssoSettingService.deleteSamlSettings("TestAccountID")).isTrue();
    assertThat(ssoSettingService.getSamlSettingsByAccountId("TestAccountID")).isNull();
    assertThat(ssoSettingService.getSamlSettingsByIdpUrl("TestURL2")).isNull();
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testNegativeTest() {
    try {
      SamlSettings samlSettings = SamlSettings.builder().build();
      ssoSettingService.saveSamlSettings(samlSettings);
      failBecauseExceptionWasNotThrown(ConstraintViolationException.class);
    } catch (javax.validation.ConstraintViolationException e) {
      assertThat(e.getConstraintViolations().size()).isEqualTo(5);
    }
  }
}
