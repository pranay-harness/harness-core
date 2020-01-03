package software.wings.signup;

import static io.harness.rule.OwnerRule.AMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.UserInvite.UserInviteBuilder.anUserInvite;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.app.DeployMode;
import software.wings.app.MainConfiguration;
import software.wings.beans.UserInvite;
import software.wings.service.intfc.SignupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.signup.SignupException;

@Slf4j
public class OnpremSignupHandlerTest extends WingsBaseTest {
  private static final String EMAIL = "abc@harness.io";
  private static final String UUID = "uuid";
  private static final String PASSWORD = "12345678";
  public static final String COMPANY_NAME = "abc";
  public static final String NAME = "test";

  @Mock private SignupService signupService;
  @Mock private UserService userService;
  @Mock private EventPublishHelper eventPublishHelper;
  @Mock private MainConfiguration configuration;

  @InjectMocks @Inject OnpremSignupHandler onpremSignupHandler;

  private UserInvite createUserInvite() {
    UserInvite userInvite = anUserInvite()
                                .withCompanyName(COMPANY_NAME)
                                .withAccountName(COMPANY_NAME)
                                .withEmail(EMAIL)
                                .withName(NAME)
                                .build();
    userInvite.setPassword(PASSWORD.toCharArray());
    return userInvite;
  }

  @Before
  public void setup() {
    doNothing().when(signupService).validateCluster();
    doNothing().when(signupService).validateEmail(EMAIL);
    when(signupService.getUserInviteByEmail(EMAIL)).thenReturn(null);
    doNothing().when(userService).sendVerificationEmail(Mockito.any(UserInvite.class), anyString(), Mockito.anyMap());

    when(userService.saveUserInvite(Mockito.any(UserInvite.class))).thenReturn(UUID);
    when(configuration.getDeployMode()).thenReturn(DeployMode.ONPREM);
    doNothing().when(eventPublishHelper).publishTrialUserSignupEvent(anyString(), anyString(), anyString());
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testNewUserInviteHandleShouldSucceed() {
    when(signupService.getUserInviteByEmail(EMAIL)).thenReturn(null);

    // Assertion
    assertThat(onpremSignupHandler.handle(createUserInvite())).isTrue();
    verify(userService, Mockito.times(1)).saveUserInvite(Mockito.any(UserInvite.class));
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testNewUserInviteCompleteShouldFail() {
    try {
      assertThat(onpremSignupHandler.completeSignup(null, null));
      fail("Expcted the above call to fail.");
    } catch (SignupException se) {
      logger.info("Expected behaviour");
    }
  }

  private void fail(String message) {
    throw new RuntimeException(message);
  }
}