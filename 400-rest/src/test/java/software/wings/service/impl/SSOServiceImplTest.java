/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.ng.core.account.AuthenticationMechanism.LDAP;
import static io.harness.ng.core.account.AuthenticationMechanism.OAUTH;
import static io.harness.ng.core.account.AuthenticationMechanism.SAML;
import static io.harness.ng.core.account.AuthenticationMechanism.USER_PASSWORD;
import static io.harness.rule.OwnerRule.RAJ;
import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.account.OauthProviderType;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Event;
import software.wings.beans.sso.OauthSettings;
import software.wings.security.authentication.SSOConfig;
import software.wings.security.saml.SamlClientService;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;

import com.coveo.saml.SamlClient;
import com.coveo.saml.SamlException;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.io.ByteArrayInputStream;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * @author Vaibhav Tulsyan
 * 14/Jun/2019
 */
@OwnedBy(HarnessTeam.PL)
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class SSOServiceImplTest extends WingsBaseTest {
  private static final String APP_ID = "app_id";

  @Mock private AuthHandler authHandler;
  @Mock private AuditServiceHelper auditServiceHelper;
  @Mock private SamlClientService samlClientService;
  @InjectMocks @Inject private SSOService ssoService;
  @InjectMocks @Inject private AccountService accountService;
  @InjectMocks @Inject private SSOSettingService ssoSettingService;

  @Test
  @Owner(developers = UJJAWAL)
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
    accountService.save(account, false);

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
  @Owner(developers = RAJ)
  @Category(UnitTests.class)
  public void test_deleteSamlConfiguration() throws SamlException {
    String accountId = "test";
    Account account = Account.Builder.anAccount()
                          .withUuid(accountId)
                          .withOauthEnabled(true)
                          .withAccountName("Account 2")
                          .withLicenseInfo(getLicenseInfo())
                          .withAppId(APP_ID)
                          .withCompanyName("Account 2")
                          .withAuthenticationMechanism(USER_PASSWORD)
                          .build();

    accountService.save(account, false);
    ssoService.uploadOauthConfiguration(
        accountId, "", ImmutableSet.of(OauthProviderType.GOOGLE, OauthProviderType.BITBUCKET));
    SamlClient samlClient = mock(SamlClient.class);
    doReturn(samlClient).when(samlClientService).getSamlClient(anyString(), anyString());
    doReturn("https://harness.onelogin.com").when(samlClient).getIdentityProviderUrl();

    // Upload SAML config and enable
    ssoService.uploadSamlConfiguration(
        accountId, new ByteArrayInputStream("test data".getBytes()), "test", "", false, "", "");
    ssoService.setAuthenticationMechanism(accountId, SAML);
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(SAML);
    assertThat(account.isOauthEnabled()).isFalse();

    ssoService.deleteSamlConfiguration(accountId);
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(USER_PASSWORD);
    assertThat(account.isOauthEnabled()).isTrue();

    accountService.delete(account.getUuid());
  }

  @Test
  @Owner(developers = RAJ)
  @Category(UnitTests.class)
  public void test_deleteSamlConfigurationWithJustUsernamePassword() throws SamlException {
    String accountId = "test";
    Account account = Account.Builder.anAccount()
                          .withUuid(accountId)
                          .withOauthEnabled(false)
                          .withAccountName("Account 2")
                          .withLicenseInfo(getLicenseInfo())
                          .withAppId(APP_ID)
                          .withCompanyName("Account 2")
                          .withAuthenticationMechanism(USER_PASSWORD)
                          .build();

    accountService.save(account, false);
    SamlClient samlClient = mock(SamlClient.class);
    doReturn(samlClient).when(samlClientService).getSamlClient(anyString(), anyString());
    doReturn("https://harness.onelogin.com").when(samlClient).getIdentityProviderUrl();

    // Upload SAML config and enable
    ssoService.uploadSamlConfiguration(
        accountId, new ByteArrayInputStream("test data".getBytes()), "test", "", false, "", "");
    ssoService.setAuthenticationMechanism(accountId, SAML);
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(SAML);
    assertThat(account.isOauthEnabled()).isFalse();

    ssoService.deleteSamlConfiguration(accountId);
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(USER_PASSWORD);
    assertThat(account.isOauthEnabled()).isFalse();

    accountService.delete(account.getUuid());
  }

  @Test
  @Owner(developers = UJJAWAL)
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
    accountService.save(account, false);
    ssoService.setAuthenticationMechanism(account.getUuid(), SAML);
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(SAML);
    assertThat(account.isOauthEnabled()).isFalse();
    accountService.delete(account.getUuid());
  }

  @Test
  @Owner(developers = UJJAWAL)
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
    accountService.save(account, false);
    ssoService.setAuthenticationMechanism(account.getUuid(), LDAP);
    account = accountService.get(account.getUuid());
    assertThat(account.getAuthenticationMechanism()).isEqualTo(LDAP);
    assertThat(account.isOauthEnabled()).isFalse();
    accountService.delete(account.getUuid());
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testAccessManagement() {
    Account account = Account.Builder.anAccount()
                          .withUuid("Account 3")
                          .withOauthEnabled(false)
                          .withAccountName("Account 3")
                          .withLicenseInfo(getLicenseInfo())
                          .withAppId(APP_ID)
                          .withCompanyName("Account 3")
                          .withAuthenticationMechanism(USER_PASSWORD)
                          .build();
    accountService.save(account, false);
    doNothing().when(authHandler).authorizeAccountPermission(anyList());
    SSOConfig accountAccessManagementSettings = ssoService.getAccountAccessManagementSettings(account.getUuid());
    assertThat(accountAccessManagementSettings).isNotNull();

    doThrow(new InvalidRequestException("INVALID")).when(authHandler).authorizeAccountPermission(anyList());
    try {
      ssoService.getAccountAccessManagementSettings(account.getUuid());
      assertThat(1 == 2).isTrue();
    } catch (InvalidRequestException ex) {
      assertThat(ex.getCode()).isEqualTo(ErrorCode.USER_NOT_AUTHORIZED);
    }
  }

  @Test
  @Owner(developers = UJJAWAL)
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
    accountService.save(account, false);
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

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testUploadOauthSettingAuditing() {
    Account account = Account.Builder.anAccount()
                          .withUuid("Account 4")
                          .withOauthEnabled(false)
                          .withAccountName("Account 4")
                          .withLicenseInfo(getLicenseInfo())
                          .withAppId(APP_ID)
                          .withCompanyName("Account 4")
                          .withAuthenticationMechanism(USER_PASSWORD)
                          .build();
    accountService.save(account, false);
    OauthSettings response =
        ssoSettingService.saveOauthSettings(OauthSettings.builder()
                                                .accountId(account.getUuid())
                                                .allowedProviders(Sets.newHashSet(OauthProviderType.GITHUB))
                                                .displayName("Some display name")
                                                .build());

    verify(auditServiceHelper, times(1))
        .reportForAuditingUsingAccountId(
            eq(account.getUuid()), eq(null), any(OauthSettings.class), eq(Event.Type.CREATE));
  }
}
