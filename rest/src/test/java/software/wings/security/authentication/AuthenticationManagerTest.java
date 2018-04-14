package software.wings.security.authentication;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Account;
import software.wings.beans.ErrorCode;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.saml.SamlClientService;
import software.wings.security.saml.SamlRequest;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserService;

import java.util.Arrays;

public class AuthenticationManagerTest extends WingsBaseTest {
  @Mock private PasswordBasedAuthHandler PASSWORD_BASED_AUTH_HANDLER;
  @Mock private SamlBasedAuthHandler SAML_BASED_AUTH_HANDLER;
  @Mock private AuthenticationUtil AUTHENTICATION_UTL;
  @Mock private SamlClientService SAML_CLIENT_SERVICE;
  @Mock private MainConfiguration MAIN_CONFIGURATION;
  @Mock private UserService USER_SERVICE;
  @Mock private WingsPersistence WINGS_PERSISTENCE;
  @Mock private AccountService ACCOUNT_SERVICE;
  @Mock private SSOSettingService SSO_SETTING_SERVICE;

  @Inject @InjectMocks private AuthenticationManager authenticationManager;

  @Test
  public void getAuthenticationMechanism() {
    try {
      authenticationManager.getAuthenticationMechanism(null);
      Assertions.failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      Assertions.assertThat(e.getMessage()).isEqualTo(ErrorCode.USER_DOES_NOT_EXIST.name());
    }

    try {
      authenticationManager.getAuthenticationMechanism("fakeUser");
      Assertions.failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      Assertions.assertThat(e.getMessage()).isEqualTo(ErrorCode.USER_DOES_NOT_EXIST.name());
    }

    User mockUser = mock(User.class);
    Account account1 = mock(Account.class);
    Account account2 = mock(Account.class);

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1, account2));
    Mockito.when(AUTHENTICATION_UTL.getUser("testUser")).thenReturn(mockUser);
    Assertions.assertThat(authenticationManager.getAuthenticationMechanism("testUser"))
        .isEqualTo(AuthenticationMechanism.USER_PASSWORD);

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    when(account1.getAuthenticationMechanism()).thenReturn(AuthenticationMechanism.USER_PASSWORD);
    Assertions.assertThat(authenticationManager.getAuthenticationMechanism("testUser"))
        .isEqualTo(AuthenticationMechanism.USER_PASSWORD);

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    when(account1.getAuthenticationMechanism()).thenReturn(AuthenticationMechanism.SAML);
    Assertions.assertThat(authenticationManager.getAuthenticationMechanism("testUser"))
        .isEqualTo(AuthenticationMechanism.SAML);
  }

  @Test
  public void getLoginTypeResponse() {
    try {
      authenticationManager.getLoginTypeResponse(null);
      Assertions.failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      Assertions.assertThat(e.getMessage()).isEqualTo(ErrorCode.USER_DOES_NOT_EXIST.name());
    }

    try {
      authenticationManager.getLoginTypeResponse("fakeUser");
      Assertions.failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      Assertions.assertThat(e.getMessage()).isEqualTo(ErrorCode.USER_DOES_NOT_EXIST.name());
    }

    User mockUser = mock(User.class);
    Account account1 = mock(Account.class);
    Account account2 = mock(Account.class);

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1, account2));
    Mockito.when(AUTHENTICATION_UTL.getUser("testUser")).thenReturn(mockUser);
    LoginTypeResponse loginTypeResponse = authenticationManager.getLoginTypeResponse("testUser");
    Assertions.assertThat(loginTypeResponse.getAuthenticationMechanism())
        .isEqualTo(AuthenticationMechanism.USER_PASSWORD);
    Assertions.assertThat(loginTypeResponse.getSamlRequest()).isNull();

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1, account2));
    Mockito.when(AUTHENTICATION_UTL.getUser("testUser")).thenReturn(mockUser);
    Assertions.assertThat(authenticationManager.getAuthenticationMechanism("testUser"))
        .isEqualTo(AuthenticationMechanism.USER_PASSWORD);

    loginTypeResponse = authenticationManager.getLoginTypeResponse("testUser");
    Assertions.assertThat(loginTypeResponse.getAuthenticationMechanism())
        .isEqualTo(AuthenticationMechanism.USER_PASSWORD);
    Assertions.assertThat(loginTypeResponse.getSamlRequest()).isNull();

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    when(account1.getAuthenticationMechanism()).thenReturn(AuthenticationMechanism.SAML);
    SamlRequest samlRequest = new SamlRequest();
    samlRequest.setIdpRedirectUrl("TestURL");
    when(SAML_CLIENT_SERVICE.generateSamlRequest(mockUser)).thenReturn(samlRequest);
    loginTypeResponse = authenticationManager.getLoginTypeResponse("testUser");
    Assertions.assertThat(loginTypeResponse.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.SAML);
    Assertions.assertThat(loginTypeResponse.getSamlRequest()).isNotNull();
    SamlRequest receivedRequest = loginTypeResponse.getSamlRequest();
    Assertions.assertThat(receivedRequest.getIdpRedirectUrl()).isEqualTo("TestURL");
  }

  @Test
  public void authenticate() {
    User mockUser = spy(new User());
    mockUser.setUuid("TestUUID");
    PortalConfig portalConfig = mock(PortalConfig.class);
    when(portalConfig.getAuthTokenExpiryInMillis()).thenReturn(System.currentTimeMillis());
    when(MAIN_CONFIGURATION.getPortal()).thenReturn(portalConfig);
    Account account1 = mock(Account.class);
    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    Mockito.when(AUTHENTICATION_UTL.getUser("testUser@test.com")).thenReturn(mockUser);

    when(PASSWORD_BASED_AUTH_HANDLER.authenticate(Matchers.anyString())).thenReturn(mockUser);
    User user = authenticationManager.defaultLogin("testUser@test.com");
    Assertions.assertThat(user.getToken()).isNotEmpty();
  }

  @Test
  public void extractToken() {
    try {
      authenticationManager.extractToken("fakeData", "Basic");
      Assertions.failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      Assertions.assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_TOKEN.name());
    }

    String token = authenticationManager.extractToken("Basic testData", "Basic");
    Assertions.assertThat(token).isEqualTo("testData");
  }
}
