package software.wings.security.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.UserService;

public class PasswordBasedAuthHandlerTest {
  @Mock private MainConfiguration configuration;

  @Mock private UserService userService;

  @Mock private WingsPersistence wingsPersistence;

  @InjectMocks @Inject PasswordBasedAuthHandler authHandler;

  @Before
  public void setUp() {
    initMocks(this);
    authHandler = spy(authHandler);
  }

  @Test
  public void testInvalidArgument() {
    try {
      assertThat(authHandler.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.USER_PASSWORD);
      authHandler.authenticate();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_ARGUMENT.name());
    }

    try {
      authHandler.authenticate("test");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_ARGUMENT.name());
    }

    try {
      authHandler.authenticate("test", "test1", "test2");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_ARGUMENT.name());
    }
  }

  @Test
  public void testBasicTokenValidationNoUserFound() {
    try {
      doReturn(null).when(authHandler).getUser(anyString());
      authHandler.authenticate("admin@harness.io", "admin");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.USER_DOES_NOT_EXIST.name());
    }
  }

  @Test
  public void testBasicTokenValidationEmailNotVerified() {
    try {
      User user = new User();
      doReturn(user).when(authHandler).getUser(anyString());
      authHandler.authenticate("admin@harness.io", "admin");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED.name());
    }
  }

  @Test
  public void testBasicTokenValidationInvalidCredentials() {
    try {
      User mockUser = mock(User.class);
      when(mockUser.isEmailVerified()).thenReturn(true);
      doReturn(mockUser).when(authHandler).getUser(anyString());
      when(mockUser.getPasswordHash()).thenReturn("$2a$10$Rf/.q4HvUkS7uG2Utdkk7.jLnqnkck5ruH/vMrHjGVk4R9mL8nQE2");
      authHandler.authenticate("admin@harness.io", "admintest");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_CREDENTIAL.name());
    }
  }

  @Test
  public void testBasicTokenValidationValidCredentials() {
    User mockUser = new User();
    mockUser.setEmailVerified(true);
    mockUser.setUuid("TestUID");
    mockUser.setPasswordHash("$2a$10$Rf/.q4HvUkS7uG2Utdkk7.jLnqnkck5ruH/vMrHjGVk4R9mL8nQE2");
    when(configuration.getPortal()).thenReturn(mock(PortalConfig.class));
    doReturn(mockUser).when(authHandler).getUser(anyString());
    User user = authHandler.authenticate("admin@harness.io", "admin");
    assertThat(user).isNotNull();
  }
}
