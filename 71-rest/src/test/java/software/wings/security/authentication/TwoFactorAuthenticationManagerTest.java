package software.wings.security.authentication;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.rule.OwnerRule.ANKIT;
import static io.harness.rule.OwnerRule.RUSHABH;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.security.authentication.TwoFactorAuthenticationMechanism.TOTP;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.rule.Repeat;
import org.joda.time.DateTimeUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.Event;
import software.wings.beans.LicenseInfo;
import software.wings.beans.User;
import software.wings.features.TwoFactorAuthenticationFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.licensing.LicenseService;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.authentication.TwoFactorAuthenticationSettings.TwoFactorAuthenticationSettingsBuilder;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;
import software.wings.utils.WingsTestConstants;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

public class TwoFactorAuthenticationManagerTest extends WingsBaseTest {
  @Mock UserService userService;
  @Mock AuthService authService;
  @Inject @InjectMocks TOTPAuthHandler totpAuthHandler;
  @Inject @InjectMocks TwoFactorAuthenticationManager twoFactorAuthenticationManager;
  @Mock AuthenticationUtils authenticationUtils;
  @Inject private AccountService accountService;
  @Inject private LicenseService licenseService;
  @Mock private AuditServiceHelper auditServiceHelper;
  @Inject @Named(TwoFactorAuthenticationFeature.FEATURE_NAME) private PremiumFeature twoFactorAuthenticationFeature;

  @Test
  @Owner(developers = RUSHABH)
  @Repeat(times = 5, successes = 1)
  @Category(UnitTests.class)
  public void shouldTwoFactorAuthenticationUsingTOTP() {
    try {
      TwoFactorAuthHandler handler = twoFactorAuthenticationManager.getTwoFactorAuthHandler(TOTP);
      User user = spy(new User());
      when(user.getDefaultAccountId()).thenReturn("kmpySmUISimoRrJL6NL73w");
      when(user.getUuid()).thenReturn("kmpySmUISimoRrJL6NL73w");
      when(userService.verifyJWTToken(anyString(), any(JWT_CATEGORY.class))).thenReturn(user);
      String totpSecretKey = TimeBasedOneTimePasswordUtil.generateBase32Secret();
      user.setTotpSecretKey(totpSecretKey);
      doReturn(TOTP).when(user).getTwoFactorAuthenticationMechanism();
      String encryptedCode = null;

      for (int t = 1; t < 60; t++) {
        DateTimeUtils.setCurrentMillisOffset(t * 1000);
        int i = -5000;
        while (i < 6000) {
          long currentTime = DateTimeUtils.currentTimeMillis();
          long timeWithLag = currentTime + i;
          log().info("Running test with time lag: [{}],currentTime=[{}],timeWithLag=[{}]", i, new Date(currentTime),
              new Date(timeWithLag));
          String code = TimeBasedOneTimePasswordUtil.generateNumberString(totpSecretKey, timeWithLag, 30);
          User authenticatedUser = spy(new User());
          authenticatedUser.setToken("ValidToken");

          when(authService.generateBearerTokenForUser(user)).thenReturn(authenticatedUser);
          encryptedCode = encodeBase64("testJWTToken:" + code);
          assertThat(twoFactorAuthenticationManager.authenticate(encryptedCode)).isEqualTo(authenticatedUser);
          i = i + 1000;
        }
      }
      try {
        when(userService.verifyJWTToken(anyString(), any(JWT_CATEGORY.class))).thenReturn(null);
        twoFactorAuthenticationManager.authenticate(encryptedCode);
        failBecauseExceptionWasNotThrown(WingsException.class);
      } catch (WingsException e) {
        assertThat(e).hasMessage(ErrorCode.USER_DOES_NOT_EXIST.name());
      }

      try {
        when(userService.verifyJWTToken(anyString(), any(JWT_CATEGORY.class))).thenReturn(user);
        user.setTotpSecretKey(null);

        twoFactorAuthenticationManager.authenticate(encryptedCode);
        failBecauseExceptionWasNotThrown(WingsException.class);
      } catch (WingsException e) {
        assertThat(e).hasMessage(ErrorCode.INVALID_TWO_FACTOR_AUTHENTICATION_CONFIGURATION.name());
      }

      try {
        when(userService.verifyJWTToken(anyString(), any(JWT_CATEGORY.class))).thenReturn(user);
        user.setTotpSecretKey(totpSecretKey);
        encryptedCode = encodeBase64("testJWTToken:1234");
        twoFactorAuthenticationManager.authenticate(encryptedCode);
        failBecauseExceptionWasNotThrown(WingsException.class);
      } catch (WingsException e) {
        assertThat(e).hasMessage(ErrorCode.INVALID_TOTP_TOKEN.name());
      }

      try {
        when(userService.verifyJWTToken(anyString(), any(JWT_CATEGORY.class))).thenReturn(user);
        String code =
            TimeBasedOneTimePasswordUtil.generateNumberString(totpSecretKey, System.currentTimeMillis() - 100000, 30);
        user.setTotpSecretKey(totpSecretKey);
        encryptedCode = encodeBase64("testJWTToken:" + code);
        twoFactorAuthenticationManager.authenticate(encryptedCode);
        failBecauseExceptionWasNotThrown(WingsException.class);
      } catch (WingsException e) {
        assertThat(e).hasMessage(ErrorCode.INVALID_TOTP_TOKEN.name());
      }

      try {
        when(userService.verifyJWTToken(anyString(), any(JWT_CATEGORY.class))).thenReturn(user);
        user.setTotpSecretKey(totpSecretKey);
        encryptedCode = encodeBase64("testJWTToken:faketoken");
        twoFactorAuthenticationManager.authenticate(encryptedCode);
        failBecauseExceptionWasNotThrown(WingsException.class);
      } catch (WingsException e) {
        assertThat(e).hasMessage(ErrorCode.UNKNOWN_ERROR.name());
      }

    } catch (GeneralSecurityException e) {
      fail(e.getMessage());
    } finally {
      DateTimeUtils.setCurrentMillisOffset(0);
    }
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void shouldCreateTwoFactorAuthenticationSettingsTotp() {
    User user = spy(new User());
    Account account = mock(Account.class);
    when(account.getCompanyName()).thenReturn("TestCompany");
    when(authenticationUtils.getDefaultAccount(user)).thenReturn(account);

    TwoFactorAuthenticationSettings settings =
        twoFactorAuthenticationManager.createTwoFactorAuthenticationSettings(user, TOTP);
    assertThat(settings.getMechanism()).isEqualTo(TOTP);
    assertThat(settings.isTwoFactorAuthenticationEnabled()).isFalse();
    assertThat(settings.getTotpSecretKey()).isNotEmpty();
    assertThat(settings.getTotpqrurl()).isNotEmpty();
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldOverrideTwoFactorAuthentication() {
    Account account = getAccount(AccountType.PAID, false);
    accountService.save(account, false);

    TwoFactorAdminOverrideSettings twoFactorAdminOverrideSettings = new TwoFactorAdminOverrideSettings(true);

    when(userService.overrideTwoFactorforAccount(
             account.getUuid(), twoFactorAdminOverrideSettings.isAdminOverrideTwoFactorEnabled()))
        .thenReturn(true);

    assertThat(twoFactorAuthenticationManager.overrideTwoFactorAuthentication(
                   account.getUuid(), twoFactorAdminOverrideSettings))
        .isTrue();
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldNotEnableTwoFactorAuthenticationForAccountWith2FAFeatureUnavailable() {
    Account account = getAccount(AccountType.COMMUNITY, false);
    accountService.save(account, false);

    TwoFactorAdminOverrideSettings twoFactorAdminOverrideSettings = new TwoFactorAdminOverrideSettings(true);

    for (String restrictedAccountType : twoFactorAuthenticationFeature.getRestrictedAccountTypes()) {
      LicenseInfo newLicenseInfo = getLicenseInfo();
      newLicenseInfo.setAccountType(restrictedAccountType);
      licenseService.updateAccountLicense(account.getUuid(), newLicenseInfo);
      try {
        twoFactorAuthenticationManager.overrideTwoFactorAuthentication(
            account.getUuid(), twoFactorAdminOverrideSettings);
        Assert.fail("Enabled 2FA");
      } catch (WingsException e) {
        assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
      }
    }
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void shouldDisableTwoFactorAuthenticationForNoAdminEnforce() {
    Account account = accountService.save(getAccount(AccountType.PAID, false), false);

    // Original user object
    User user = getUser(true);
    user.setAccounts(Arrays.asList(account));

    // Updated user object
    User updatedUser = getUser(false);
    updatedUser.setAccounts(Arrays.asList(account));

    // Should allow disable
    twoFactorAuthenticationManager.disableTwoFactorAuthentication(user);
    assertThat(updatedUser.isTwoFactorAuthenticationEnabled()).isFalse();
    verify(auditServiceHelper, times(user.getAccounts().size()))
        .reportForAuditingUsingAccountId(anyString(), eq(null), eq(user), eq(Event.Type.DISABLE_2FA));
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldDisableTwoFactorAuthenticationForAdminEnforce() {
    Account account = accountService.save(getAccount(AccountType.PAID, true), false);

    User user = getUser(true);
    user.setAccounts(Arrays.asList(account));

    // Should not allow disable
    twoFactorAuthenticationManager.disableTwoFactorAuthentication(user);
    assertThat(user.isTwoFactorAuthenticationEnabled()).isTrue();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldDisableTwoFactorAuthenticationForMultiAccounts() {
    Account account1 = accountService.save(getAccount(AccountType.PAID, false), false);
    Account account2 = accountService.save(getAccount(AccountType.PAID, false), false);

    User user = getUser(true);
    user.setAccounts(Arrays.asList(account1, account2));

    // Should allow disable
    twoFactorAuthenticationManager.disableTwoFactorAuthentication(user);
    assertThat(user.isTwoFactorAuthenticationEnabled()).isFalse();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void shouldDisableTwoFactorAuthenticationForNoAccounts() {
    User user = getUser(true);
    user.setAccounts(null);

    // Should allow disable
    twoFactorAuthenticationManager.disableTwoFactorAuthentication(user);
    assertThat(user.isTwoFactorAuthenticationEnabled()).isTrue();
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldNotEnableTwoFactorAuthenticationForUserWhosePrimaryAccountHas2FAFeatureUnavailable() {
    Account account = getAccount(AccountType.PAID, false);
    accountService.save(account, false);

    for (String restrictedAccountType : twoFactorAuthenticationFeature.getRestrictedAccountTypes()) {
      Account primaryAccount = getAccount(restrictedAccountType, false);
      primaryAccount.setAccountName(accountService.suggestAccountName(primaryAccount.getAccountName()));
      accountService.save(primaryAccount, false);

      User user = getUser(false);
      user.setAccounts(Arrays.asList(primaryAccount, account));

      TwoFactorAuthenticationSettings settings =
          new TwoFactorAuthenticationSettingsBuilder().twoFactorAuthenticationEnabled(true).mechanism(TOTP).build();

      try {
        twoFactorAuthenticationManager.enableTwoFactorAuthenticationSettings(user, settings);
        Assert.fail();
      } catch (WingsException e) {
        assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
      }
    }
  }

  private Account getAccount(String accountType, boolean twoFactorAdminEnforced) {
    Account account = anAccount()
                          .withUuid(UUID.randomUUID().toString())
                          .withAccountName(WingsTestConstants.ACCOUNT_NAME)
                          .withCompanyName(WingsTestConstants.COMPANY_NAME)
                          .withWhitelistedDomains(Collections.emptySet())
                          .build();
    account.setTwoFactorAdminEnforced(twoFactorAdminEnforced);
    LicenseInfo license = getLicenseInfo();
    license.setAccountType(accountType);
    account.setLicenseInfo(license);

    return account;
  }

  private User getUser(boolean twoFactorEnabled) {
    User user = spy(new User());
    user.setTwoFactorAuthenticationEnabled(twoFactorEnabled);
    user.setTwoFactorAuthenticationMechanism(TOTP);
    return user;
  }
}