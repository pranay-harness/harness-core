package io.harness.signup.services;

import static io.harness.annotations.dev.HarnessTeam.GTM;
import static io.harness.rule.OwnerRule.NATHAN;
import static io.harness.rule.OwnerRule.ZHUO;
import static io.harness.signup.services.impl.SignupServiceImpl.FAILED_EVENT_NAME;
import static io.harness.signup.services.impl.SignupServiceImpl.SUCCEED_EVENT_NAME;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.account.services.AccountService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.authenticationservice.recaptcha.ReCaptchaVerifier;
import io.harness.category.element.UnitTests;
import io.harness.exception.SignupException;
import io.harness.exception.WingsException;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserRequestDTO;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.signup.SignupTestBase;
import io.harness.signup.dto.OAuthSignupDTO;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.services.impl.SignupServiceImpl;
import io.harness.signup.validator.SignupValidator;
import io.harness.telemetry.TelemetryReporter;
import io.harness.user.remote.UserClient;

import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(GTM)
public class SignupServiceImplTest extends SignupTestBase {
  @InjectMocks SignupServiceImpl signupServiceImpl;
  @Mock SignupValidator signupValidator;
  @Mock AccountService accountService;
  @Mock UserClient userClient;
  @Mock ReCaptchaVerifier reCaptchaVerifier;
  @Mock TelemetryReporter telemetryReporter;

  private static final String EMAIL = "test@test.com";
  private static final String INVALID_EMAIL = "test";
  private static final String PASSWORD = "admin12345";
  private static final String ACCOUNT_ID = "account1";

  @Test
  @Owner(developers = NATHAN)
  @Category(UnitTests.class)
  public void testSignup() throws IOException {
    SignupDTO signupDTO = SignupDTO.builder().email(EMAIL).password(PASSWORD).build();
    AccountDTO accountDTO = AccountDTO.builder().identifier(ACCOUNT_ID).build();
    UserInfo newUser = UserInfo.builder().email(EMAIL).build();

    doNothing().when(signupValidator).validateSignup(any(SignupDTO.class));
    when(accountService.createAccount(signupDTO)).thenReturn(accountDTO);

    Call<RestResponse<UserInfo>> createUserCall = mock(Call.class);
    when(createUserCall.execute()).thenReturn(Response.success(new RestResponse<>(newUser)));
    when(userClient.createNewUser(any(UserRequestDTO.class))).thenReturn(createUserCall);

    UserInfo returnedUser = signupServiceImpl.signup(signupDTO, null);

    verify(reCaptchaVerifier, times(1)).verifyInvisibleCaptcha(anyString());
    verify(telemetryReporter, times(1))
        .sendTrackEvent(
            eq(SUCCEED_EVENT_NAME), eq(EMAIL), eq(ACCOUNT_ID), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
    verify(telemetryReporter, times(1)).sendIdentifyEvent(eq(EMAIL), any(), any());
    assertThat(returnedUser.getEmail()).isEqualTo(newUser.getEmail());
  }

  @Test
  @Owner(developers = NATHAN)
  @Category(UnitTests.class)
  public void testSignupOAuth() throws IOException {
    String name = "testName";
    OAuthSignupDTO oAuthSignupDTO = OAuthSignupDTO.builder().email(EMAIL).name(name).build();
    AccountDTO accountDTO = AccountDTO.builder().identifier(ACCOUNT_ID).build();
    UserInfo newUser = UserInfo.builder().email(EMAIL).build();

    doNothing().when(signupValidator).validateEmail(eq(EMAIL));
    when(accountService.createAccount(any(SignupDTO.class))).thenReturn(accountDTO);

    Call<RestResponse<UserInfo>> createUserCall = mock(Call.class);
    when(createUserCall.execute()).thenReturn(Response.success(new RestResponse<>(newUser)));
    when(userClient.createNewOAuthUser(any(UserRequestDTO.class))).thenReturn(createUserCall);

    UserInfo returnedUser = signupServiceImpl.oAuthSignup(oAuthSignupDTO);

    verify(telemetryReporter, times(1))
        .sendTrackEvent(
            eq(SUCCEED_EVENT_NAME), eq(EMAIL), eq(ACCOUNT_ID), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
    verify(telemetryReporter, times(1)).sendIdentifyEvent(eq(EMAIL), any(), any());
    assertThat(returnedUser.getEmail()).isEqualTo(newUser.getEmail());
  }

  @Test(expected = SignupException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testSignupWithInvalidEmail() {
    SignupDTO signupDTO = SignupDTO.builder().email(INVALID_EMAIL).password(PASSWORD).build();
    doThrow(new SignupException("This email is invalid. email=" + INVALID_EMAIL))
        .when(signupValidator)
        .validateSignup(signupDTO);
    try {
      signupServiceImpl.signup(signupDTO, null);
    } catch (SignupException e) {
      verify(telemetryReporter, times(1))
          .sendTrackEvent(
              eq(FAILED_EVENT_NAME), eq(INVALID_EMAIL), any(), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
      throw e;
    }
  }

  @Test(expected = WingsException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testSignupWithInvliadReCaptcha() {
    SignupDTO signupDTO = SignupDTO.builder().email(INVALID_EMAIL).password(PASSWORD).build();
    doThrow(new WingsException("")).when(reCaptchaVerifier).verifyInvisibleCaptcha(any());
    try {
      signupServiceImpl.signup(signupDTO, null);
    } catch (WingsException e) {
      verify(telemetryReporter, times(1))
          .sendTrackEvent(
              eq(FAILED_EVENT_NAME), eq(INVALID_EMAIL), any(), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
      throw e;
    }
  }

  @Test(expected = SignupException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testSignupOAuthWithInvalidEmail() {
    OAuthSignupDTO oAuthSignupDTO = OAuthSignupDTO.builder().email(INVALID_EMAIL).name("name").build();
    doThrow(new SignupException("This email is invalid. email=" + INVALID_EMAIL))
        .when(signupValidator)
        .validateEmail(oAuthSignupDTO.getEmail());
    try {
      signupServiceImpl.oAuthSignup(oAuthSignupDTO);
    } catch (SignupException e) {
      verify(telemetryReporter, times(1))
          .sendTrackEvent(
              eq(FAILED_EVENT_NAME), eq(INVALID_EMAIL), any(), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
      throw e;
    }
  }
}
