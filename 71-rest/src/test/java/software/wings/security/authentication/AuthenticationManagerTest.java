package software.wings.security.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.FeatureName.LOGIN_PROMPT_WHEN_NO_USER;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.commons.codec.binary.Base64;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.security.saml.SSORequest;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserService;

import java.util.Arrays;
import java.util.EnumSet;

public class AuthenticationManagerTest extends WingsBaseTest {
  public static final String NON_EXISTING_USER = "nonExistingUser";
  @Mock private PasswordBasedAuthHandler PASSWORD_BASED_AUTH_HANDLER;
  @Mock private SamlBasedAuthHandler SAML_BASED_AUTH_HANDLER;
  @Mock private SamlClientService SAML_CLIENT_SERVICE;
  @Mock private MainConfiguration MAIN_CONFIGURATION;
  @Mock private UserService USER_SERVICE;
  @Mock private WingsPersistence WINGS_PERSISTENCE;
  @Mock private AccountService ACCOUNT_SERVICE;
  @Mock private SSOSettingService SSO_SETTING_SERVICE;
  @Mock private AuthenticationUtil AUTHENTICATION_UTL;
  @Mock private AuthService AUTHSERVICE;
  @Mock private FeatureFlagService FEATURE_FLAG_SERVICE;

  @Inject @InjectMocks private AuthenticationManager authenticationManager;

  @Test
  @Category(UnitTests.class)
  public void getAuthenticationMechanism() {
    User mockUser = mock(User.class);
    Account account1 = mock(Account.class);
    Account account2 = mock(Account.class);

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1, account2));
    when(AUTHENTICATION_UTL.getUser("testUser", WingsException.USER)).thenReturn(mockUser);
    assertThat(authenticationManager.getAuthenticationMechanism("testUser"))
        .isEqualTo(AuthenticationMechanism.USER_PASSWORD);

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    when(account1.getAuthenticationMechanism()).thenReturn(AuthenticationMechanism.USER_PASSWORD);
    assertThat(authenticationManager.getAuthenticationMechanism("testUser"))
        .isEqualTo(AuthenticationMechanism.USER_PASSWORD);

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    when(account1.getAuthenticationMechanism()).thenReturn(AuthenticationMechanism.SAML);
    assertThat(authenticationManager.getAuthenticationMechanism("testUser")).isEqualTo(AuthenticationMechanism.SAML);
  }

  @Test
  @Category(UnitTests.class)
  public void getLoginTypeResponse() {
    User mockUser = mock(User.class);
    Account account1 = mock(Account.class);
    Account account2 = mock(Account.class);

    when(FEATURE_FLAG_SERVICE.isEnabled(LOGIN_PROMPT_WHEN_NO_USER, GLOBAL_ACCOUNT_ID)).thenReturn(true);
    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1, account2));
    when(mockUser.isEmailVerified()).thenReturn(true);
    when(AUTHENTICATION_UTL.getUser(Matchers.same(NON_EXISTING_USER), any(EnumSet.class)))
        .thenThrow(new WingsException(ErrorCode.USER_DOES_NOT_EXIST));
    LoginTypeResponse loginTypeResponse = authenticationManager.getLoginTypeResponse(NON_EXISTING_USER);
    assertThat(loginTypeResponse.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.USER_PASSWORD);
    assertThat(loginTypeResponse.getSSORequest()).isNull();

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1, account2));
    when(AUTHENTICATION_UTL.getUser(Matchers.anyString(), any(EnumSet.class))).thenReturn(mockUser);
    loginTypeResponse = authenticationManager.getLoginTypeResponse("testUser");
    assertThat(loginTypeResponse.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.USER_PASSWORD);
    assertThat(loginTypeResponse.getSSORequest()).isNull();

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1, account2));
    when(AUTHENTICATION_UTL.getUser("testUser", WingsException.USER)).thenReturn(mockUser);
    assertThat(authenticationManager.getAuthenticationMechanism("testUser"))
        .isEqualTo(AuthenticationMechanism.USER_PASSWORD);

    loginTypeResponse = authenticationManager.getLoginTypeResponse("testUser");
    assertThat(loginTypeResponse.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.USER_PASSWORD);
    assertThat(loginTypeResponse.getSSORequest()).isNull();

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    when(account1.getAuthenticationMechanism()).thenReturn(AuthenticationMechanism.SAML);
    SSORequest SSORequest = new SSORequest();
    SSORequest.setIdpRedirectUrl("TestURL");
    when(SAML_CLIENT_SERVICE.generateSamlRequest(mockUser)).thenReturn(SSORequest);
    loginTypeResponse = authenticationManager.getLoginTypeResponse("testUser");
    assertThat(loginTypeResponse.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.SAML);
    assertThat(loginTypeResponse.getSSORequest()).isNotNull();
    SSORequest receivedRequest = loginTypeResponse.getSSORequest();
    assertThat(receivedRequest.getIdpRedirectUrl()).isEqualTo("TestURL");
  }

  @Test
  @Category(UnitTests.class)
  public void testGetLoginType_emailUnverified_shouldFail() {
    User mockUser = mock(User.class);
    Account account1 = mock(Account.class);

    when(FEATURE_FLAG_SERVICE.isEnabled(LOGIN_PROMPT_WHEN_NO_USER, GLOBAL_ACCOUNT_ID)).thenReturn(false);
    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));

    when(AUTHENTICATION_UTL.getUser(Matchers.anyString(), any(EnumSet.class))).thenReturn(mockUser);
    try {
      authenticationManager.getLoginTypeResponse("testUser");
      fail("Exception is expected if the user email is not verified.");
    } catch (WingsException e) {
      // Exception expected.
    }
  }

  @Test
  @Category(UnitTests.class)
  public void authenticate() {
    User mockUser = spy(new User());
    AuthenticationResponse authenticationResponse = spy(new AuthenticationResponse(mockUser));
    mockUser.setUuid("TestUUID");
    PortalConfig portalConfig = mock(PortalConfig.class);
    when(portalConfig.getAuthTokenExpiryInMillis()).thenReturn(System.currentTimeMillis());
    when(MAIN_CONFIGURATION.getPortal()).thenReturn(portalConfig);
    Account account1 = mock(Account.class);
    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    when(AUTHENTICATION_UTL.getUser("testUser@test.com", WingsException.USER)).thenReturn(mockUser);

    when(PASSWORD_BASED_AUTH_HANDLER.authenticate(Matchers.anyString(), Matchers.anyString()))
        .thenReturn(authenticationResponse);
    User authenticatedUser = mock(User.class);
    when(authenticatedUser.getToken()).thenReturn("TestToken");
    when(AUTHSERVICE.generateBearerTokenForUser(mockUser)).thenReturn(authenticatedUser);
    User user = authenticationManager.defaultLogin(Base64.encodeBase64String("testUser@test.com:password".getBytes()));
    assertThat(user.getToken()).isEqualTo("TestToken");
    assertThat(authenticatedUser.getLastLogin() != 0L);
    assertThat(authenticatedUser.getLastLogin() <= System.currentTimeMillis());
  }

  @Test
  @Category(UnitTests.class)
  public void testFakeTokens() {
    try {
      authenticationManager.defaultLogin("FakeToken");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_CREDENTIAL.name());
    }

    try {
      authenticationManager.defaultLogin(Base64.encodeBase64String("testUser@test.com".getBytes()));
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_CREDENTIAL.name());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void extractToken() {
    try {
      authenticationManager.extractToken("fakeData", "Basic");
      Assertions.failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_TOKEN.name());
    }

    String token = authenticationManager.extractToken("Basic testData", "Basic");
    assertThat(token).isEqualTo("testData");
  }
}
