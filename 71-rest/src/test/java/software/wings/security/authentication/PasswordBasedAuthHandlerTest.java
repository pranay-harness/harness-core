package software.wings.security.authentication;

import static io.harness.rule.OwnerRule.RUSHABH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.loginSettings.LoginSettingsService;
import software.wings.beans.loginSettings.UserLockoutInfo;
import software.wings.dl.WingsPersistence;
import software.wings.security.authentication.recaptcha.FailedLoginAttemptCountChecker;
import software.wings.security.authentication.recaptcha.MaxLoginAttemptExceededException;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

public class PasswordBasedAuthHandlerTest extends CategoryTest {
  @Mock private MainConfiguration configuration;
  @Mock private UserService userService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private LoginSettingsService loginSettingsService;
  @Mock private AuthenticationUtils authenticationUtils;
  @Mock private AccountService accountService;
  @Mock private FailedLoginAttemptCountChecker failedLoginAttemptCountChecker;
  @Mock private DomainWhitelistCheckerService domainWhitelistCheckerService;

  @InjectMocks @Inject @Spy PasswordBasedAuthHandler authHandler;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
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
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
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
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testBasicTokenValidationEmailNotVerified() {
    try {
      User user = new User();
      user.setDefaultAccountId("kmpySmUISimoRrJL6NL73w");
      user.setUuid("kmpySmUISimoRrJL6NL73w");
      doReturn(user).when(authHandler).getUser(anyString());
      authHandler.authenticate("admin@harness.io", "admin");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED.name());
    }
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testBasicTokenValidationInvalidCredentials() throws MaxLoginAttemptExceededException {
    try {
      doNothing().when(failedLoginAttemptCountChecker).check(Mockito.any(User.class));

      User mockUser = mock(User.class);
      when(mockUser.isEmailVerified()).thenReturn(true);
      when(mockUser.getUserLockoutInfo()).thenReturn(new UserLockoutInfo());
      when(mockUser.getDefaultAccountId()).thenReturn("kmpySmUISimoRrJL6NL73w");
      when(mockUser.getUuid()).thenReturn("kmpySmUISimoRrJL6NL73w");
      doNothing().when(loginSettingsService).updateUserLockoutInfo(any(User.class), any(Account.class), anyInt());
      doReturn(mockUser).when(authHandler).getUser(anyString());
      when(mockUser.getPasswordHash()).thenReturn("$2a$10$Rf/.q4HvUkS7uG2Utdkk7.jLnqnkck5ruH/vMrHjGVk4R9mL8nQE2");
      when(domainWhitelistCheckerService.isDomainWhitelisted(mockUser)).thenReturn(true);
      authHandler.authenticate("admin@harness.io", "admintest");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_CREDENTIAL);
    }
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testFailedAttemptLimitExceeded() throws MaxLoginAttemptExceededException {
    try {
      doThrow(new MaxLoginAttemptExceededException(3, 4))
          .when(failedLoginAttemptCountChecker)
          .check(Mockito.any(User.class));

      User mockUser = mock(User.class);
      when(mockUser.getDefaultAccountId()).thenReturn("kmpySmUISimoRrJL6NL73w");
      when(mockUser.getUuid()).thenReturn("kmpySmUISimoRrJL6NL73w");
      when(mockUser.isEmailVerified()).thenReturn(true);
      when(mockUser.getUserLockoutInfo()).thenReturn(new UserLockoutInfo());
      doNothing().when(loginSettingsService).updateUserLockoutInfo(any(User.class), any(Account.class), anyInt());
      doReturn(mockUser).when(authHandler).getUser(anyString());
      when(mockUser.getPasswordHash()).thenReturn("$2a$10$Rf/.q4HvUkS7uG2Utdkk7.jLnqnkck5ruH/vMrHjGVk4R9mL8nQE2");
      when(domainWhitelistCheckerService.isDomainWhitelisted(mockUser)).thenReturn(true);
      authHandler.authenticate("admin@harness.io", "admintest");
      failBecauseExceptionWasNotThrown(WingsException.class);

    } catch (WingsException e) {
      assertThat(e.getCode()).isEqualTo(ErrorCode.MAX_FAILED_ATTEMPT_COUNT_EXCEEDED);
    }
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testBasicTokenValidationValidCredentials() {
    User mockUser = new User();
    mockUser.setEmailVerified(true);
    mockUser.setUuid("TestUID");
    mockUser.setDefaultAccountId("accountId");
    mockUser.setPasswordHash("$2a$10$Rf/.q4HvUkS7uG2Utdkk7.jLnqnkck5ruH/vMrHjGVk4R9mL8nQE2");
    when(configuration.getPortal()).thenReturn(mock(PortalConfig.class));
    when(authenticationUtils.getDefaultAccount(any(User.class))).thenReturn(new Account());
    when(loginSettingsService.isUserLocked(any(User.class), any(Account.class))).thenReturn(false);
    doNothing().when(loginSettingsService).updateUserLockoutInfo(any(User.class), any(Account.class), anyInt());
    doReturn(mockUser).when(authHandler).getUser(anyString());
    when(domainWhitelistCheckerService.isDomainWhitelisted(mockUser)).thenReturn(true);
    User user = authHandler.authenticate("admin@harness.io", "admin").getUser();
    assertThat(user).isNotNull();
  }
}
