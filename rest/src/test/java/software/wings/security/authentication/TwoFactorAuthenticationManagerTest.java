package software.wings.security.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import io.harness.rule.RepeatRule.Repeat;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.ErrorCode;
import software.wings.beans.User;
import software.wings.exception.WingsException;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.service.intfc.UserService;

import java.security.GeneralSecurityException;
import java.util.Base64;

public class TwoFactorAuthenticationManagerTest extends WingsBaseTest {
  @Mock UserService userService;
  @Mock AuthenticationUtil authenticationUtil;
  @Inject @InjectMocks TOTPAuthHandler totpAuthHandler;
  @Inject @InjectMocks TwoFactorAuthenticationManager twoFactorAuthenticationManager;

  @Test
  @Repeat(times = 5, successes = 1)
  public void testTwoFactorAuthenticationUsingTOTP() {
    try {
      TwoFactorAuthHandler handler =
          twoFactorAuthenticationManager.getTwoFactorAuthHandler(TwoFactorAuthenticationMechanism.TOTP);
      User user = spy(new User());
      when(userService.verifyJWTToken(anyString(), any(JWT_CATEGORY.class))).thenReturn(user);
      String totpSecretKey = TimeBasedOneTimePasswordUtil.generateBase32Secret();
      user.setTotpSecretKey(totpSecretKey);
      doReturn(TwoFactorAuthenticationMechanism.TOTP).when(user).getTwoFactorAuthenticationMechanism();
      String code = TimeBasedOneTimePasswordUtil.generateCurrentNumberString(totpSecretKey);

      User authenticatedUser = spy(new User());
      authenticatedUser.setToken("ValidToken");

      when(authenticationUtil.generateBearerTokenForUser(user)).thenReturn(authenticatedUser);
      String encryptedCode = Base64.getEncoder().encodeToString(("testJWTToken:" + code).getBytes());
      assertThat(twoFactorAuthenticationManager.authenticate(encryptedCode)).isEqualTo(authenticatedUser);

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
        encryptedCode = Base64.getEncoder().encodeToString("testJWTToken:invalid_code".getBytes());
        twoFactorAuthenticationManager.authenticate(encryptedCode);
        failBecauseExceptionWasNotThrown(WingsException.class);
      } catch (WingsException e) {
        assertThat(e).hasMessage(ErrorCode.INVALID_TOTP_TOKEN.name());
      }

    } catch (GeneralSecurityException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testCreateTwoFactorAuthenticationSettingsTotp() {
    User user = spy(new User());
    Account account = mock(Account.class);
    when(account.getCompanyName()).thenReturn("TestCompany");
    when(authenticationUtil.getPrimaryAccount(user)).thenReturn(account);

    TwoFactorAuthenticationSettings settings = twoFactorAuthenticationManager.createTwoFactorAuthenticationSettings(
        user, TwoFactorAuthenticationMechanism.TOTP);
    assertThat(settings.getMechanism()).isEqualTo(TwoFactorAuthenticationMechanism.TOTP);
    assertThat(settings.isTwoFactorAuthenticationEnabled()).isFalse();
    assertThat(settings.getTotpSecretKey()).isNotEmpty();
    assertThat(settings.getTotpqrurl()).isNotEmpty();
  }
}
