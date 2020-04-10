package software.wings.service.intfc.signup;

import static io.harness.rule.OwnerRule.AMAN;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.UserInvite.UserInviteBuilder;
import software.wings.service.intfc.SignupService;
import software.wings.service.intfc.UserService;

@Slf4j
public class SignupServiceTest extends WingsBaseTest {
  public static final String EMAIL = "test@harness.io";
  @Inject SignupService signupService;
  @Inject UserService userService;

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testInvalidEmail() {
    try {
      signupService.checkIfEmailIsValid("abc@example.com");
    } catch (Exception e) {
      assertThat(e).isNull();
    }
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testInvalidEmailNegative() {
    try {
      signupService.checkIfEmailIsValid("example");
    } catch (Exception e) {
      assertThat(e).isNotNull();
    }
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testInvalidUserCheckShouldSucceed() {
    try {
      signupService.checkIfUserInviteIsValid(null, "abc@example.com");
      fail();
    } catch (SignupException se) {
      assertThat(se.getMessage()).isEqualTo(String.format("Can not process signup for email: %s", "abc@example.com"));
    }
    try {
      signupService.checkIfUserInviteIsValid(
          UserInviteBuilder.anUserInvite().withCompleted(true).build(), "abc@example.com");
      fail();
    } catch (SignupException se) {
      assertThat(se.getMessage()).isEqualTo("User invite has already been completed. Please login");
    }
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testGetEmailShouldSucceed() {
    String signupSecretToken = userService.createSignupSecretToken(EMAIL, 10);
    assertThat(signupService.getEmail(signupSecretToken)).isEqualTo(EMAIL);

    try {
      signupSecretToken = userService.createSignupSecretToken(EMAIL, -10);
      signupService.getEmail(signupSecretToken);
      fail();
    } catch (SignupException ex) {
      logger.info("Test behaved as expected");
    }
  }

  private void fail() {
    throw new RuntimeException("Expected to fail before reaching this line");
  }
}
