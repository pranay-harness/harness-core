package software.wings.service.impl;

import static io.harness.rule.OwnerRule.VAIBHAV_TULSYAN;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.security.authentication.AuthenticationMechanism.LDAP;
import static software.wings.security.authentication.AuthenticationMechanism.OAUTH;
import static software.wings.security.authentication.AuthenticationMechanism.SAML;
import static software.wings.security.authentication.AuthenticationMechanism.USER_PASSWORD;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.sso.OauthSettings;
import software.wings.security.authentication.OauthProviderType;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;

/**
 * @author Vaibhav Tulsyan
 * 14/Jun/2019
 */
public class SSOServiceImplTest extends WingsBaseTest {
  private static final String APP_ID = "app_id";

  @Inject private SSOService ssoService;
  @Inject private AccountService accountService;
  @Inject private SSOSettingService ssoSettingService;

  @Test
  @Owner(developers = VAIBHAV_TULSYAN)
  @Category(UnitTests.class)
  public void test_OauthAndUserPasswordCoexistence() {
    Account account = Account.Builder.anAccount()
                          .withUuid("Account 1")
                          .withOauthEnabled(false)
                          .withAccountName("Account 1")
                          .withLicenseInfo(getLicenseInfo())
                          .withAppId(APP_ID)
                          .withCompanyName("Account 2")
                          .withAuthenticationMechanism(USER_PASSWORD)
                          .build();
    accountService.save(account);

    // Testcases to check logic for co-existence of USERNAME_PASSWORD and OAUTH
    // UP = USER_PASSWORD, OA = OAUTH

    // UP -> UP + OA - enable oauth
    ssoService.uploadOauthConfiguration(account.getUuid(), "", Sets.newHashSet(OauthProviderType.values()));
    ssoService.setAuthenticationMechanism(account.getUuid(), OAUTH);
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(USER_PASSWORD);
    assertThat(account.isOauthEnabled()).isTrue();

    // UP + OA -> OA - disable user password
    ssoService.setAuthenticationMechanism(account.getUuid(), OAUTH);
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(OAUTH);
    assertThat(account.isOauthEnabled()).isTrue();

    // OA -> UP + OA - enable user password
    ssoService.setAuthenticationMechanism(account.getUuid(), USER_PASSWORD);
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(USER_PASSWORD);
    assertThat(account.isOauthEnabled()).isTrue();

    // UP + OA -> UP - disable oauth
    ssoService.setAuthenticationMechanism(account.getUuid(), USER_PASSWORD);
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(USER_PASSWORD);
    assertThat(account.isOauthEnabled()).isFalse();

    accountService.delete(account.getUuid());
  }

  @Test
  @Owner(developers = VAIBHAV_TULSYAN)
  @Category(UnitTests.class)
  public void test_UserPasswordToSAMLAuthMechanismUpdate() {
    Account account = Account.Builder.anAccount()
                          .withUuid("Account 2")
                          .withOauthEnabled(false)
                          .withAccountName("Account 2")
                          .withLicenseInfo(getLicenseInfo())
                          .withAppId(APP_ID)
                          .withCompanyName("Account 2")
                          .withAuthenticationMechanism(USER_PASSWORD)
                          .build();
    accountService.save(account);
    ssoService.setAuthenticationMechanism(account.getUuid(), SAML);
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(SAML);
    assertThat(account.isOauthEnabled()).isFalse();
    accountService.delete(account.getUuid());
  }

  @Test
  @Owner(developers = VAIBHAV_TULSYAN)
  @Category(UnitTests.class)
  public void test_UserPasswordToLDAPAuthMechanismUpdate() {
    Account account = Account.Builder.anAccount()
                          .withUuid("Account 3")
                          .withOauthEnabled(false)
                          .withAccountName("Account 3")
                          .withLicenseInfo(getLicenseInfo())
                          .withAppId(APP_ID)
                          .withCompanyName("Account 3")
                          .withAuthenticationMechanism(USER_PASSWORD)
                          .build();
    accountService.save(account);
    ssoService.setAuthenticationMechanism(account.getUuid(), LDAP);
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(LDAP);
    assertThat(account.isOauthEnabled()).isFalse();
    accountService.delete(account.getUuid());
  }

  @Test
  @Owner(developers = VAIBHAV_TULSYAN)
  @Category(UnitTests.class)
  public void test_uploadOauthSetting_thenSetAuthMechanismAsOauth() {
    Account account = Account.Builder.anAccount()
                          .withUuid("Account 4")
                          .withOauthEnabled(false)
                          .withAccountName("Account 4")
                          .withLicenseInfo(getLicenseInfo())
                          .withAppId(APP_ID)
                          .withCompanyName("Account 4")
                          .withAuthenticationMechanism(USER_PASSWORD)
                          .build();
    accountService.save(account);
    OauthSettings response =
        ssoSettingService.saveOauthSettings(OauthSettings.builder()
                                                .accountId(account.getUuid())
                                                .allowedProviders(Sets.newHashSet(OauthProviderType.GITHUB))
                                                .displayName("Some display name")
                                                .build());
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(USER_PASSWORD);
    assertThat(account.isOauthEnabled()).isFalse();

    ssoService.setAuthenticationMechanism(account.getUuid(), OAUTH);
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(USER_PASSWORD);
    assertThat(account.isOauthEnabled()).isTrue();
  }
}