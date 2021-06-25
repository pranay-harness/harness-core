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

import io.harness.CategoryTest;
import io.harness.account.services.AccountService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.authenticationservice.recaptcha.ReCaptchaVerifier;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.exception.SignupException;
import io.harness.exception.UnavailableFeatureException;
import io.harness.exception.WingsException;
import io.harness.ff.FeatureFlagService;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserRequestDTO;
import io.harness.repositories.SignupVerificationTokenRepository;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.UserPrincipal;
import io.harness.signup.dto.OAuthSignupDTO;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.dto.VerifyTokenResponseDTO;
import io.harness.signup.entities.SignupVerificationToken;
import io.harness.signup.notification.EmailType;
import io.harness.signup.notification.SignupNotificationHelper;
import io.harness.signup.services.impl.SignupServiceImpl;
import io.harness.signup.validator.SignupValidator;
import io.harness.telemetry.TelemetryReporter;
import io.harness.user.remote.UserClient;

import com.google.inject.name.Named;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(GTM)
@RunWith(PowerMockRunner.class)
@PrepareForTest(SourcePrincipalContextBuilder.class)
public class SignupServiceImplTest extends CategoryTest {
  @InjectMocks SignupServiceImpl signupServiceImpl;
  @Mock FeatureFlagService featureFlagService;
  @Mock SignupValidator signupValidator;
  @Mock AccountService accountService;
  @Mock UserClient userClient;
  @Mock ReCaptchaVerifier reCaptchaVerifier;
  @Mock TelemetryReporter telemetryReporter;
  @Mock SignupNotificationHelper signupNotificationHelper;
  @Mock SignupVerificationTokenRepository verificationTokenRepository;
  @Mock @Named("NGSignupNotification") ExecutorService executorService;

  private static final String EMAIL = "test@test.com";
  private static final String INVALID_EMAIL = "test";
  private static final String PASSWORD = "admin12345";
  private static final String ACCOUNT_ID = "account1";
  private static final String VERIFY_URL = "register/verify";
  private static final String NEXT_GEN_MANAGER_URI = "http://localhost:8181/ng/#/";
  private static final String NEXT_GEN_AUTH_URI = "http://localhost:8181/auth/#/";

  @Before
  public void setup() throws IllegalAccessException {
    PowerMockito.mockStatic(SourcePrincipalContextBuilder.class);
    signupServiceImpl = new SignupServiceImpl(accountService, userClient, signupValidator, reCaptchaVerifier,
        telemetryReporter, signupNotificationHelper, featureFlagService, verificationTokenRepository, executorService,
        NEXT_GEN_MANAGER_URI, NEXT_GEN_AUTH_URI);
  }

  @Test
  @Owner(developers = NATHAN)
  @Category(UnitTests.class)
  public void testSignup() throws IOException {
    when(featureFlagService.isGlobalEnabled(FeatureName.NG_SIGNUP)).thenReturn(true);
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
    verify(executorService, times(1));
    assertThat(returnedUser.getEmail()).isEqualTo(newUser.getEmail());
  }

  @Test
  @Owner(developers = NATHAN)
  @Category(UnitTests.class)
  public void testSignupOAuth() throws IOException {
    when(featureFlagService.isGlobalEnabled(FeatureName.NG_SIGNUP)).thenReturn(true);
    String name = "testName";
    OAuthSignupDTO oAuthSignupDTO = OAuthSignupDTO.builder().email(EMAIL).name(name).build();
    AccountDTO accountDTO = AccountDTO.builder().identifier(ACCOUNT_ID).build();
    UserInfo newUser = UserInfo.builder().email(EMAIL).build();

    doNothing().when(signupValidator).validateEmail(eq(EMAIL));
    when(accountService.createAccount(any(SignupDTO.class))).thenReturn(accountDTO);

    Call<RestResponse<UserInfo>> createUserCall = mock(Call.class);
    when(createUserCall.execute()).thenReturn(Response.success(new RestResponse<>(newUser)));
    when(userClient.createNewOAuthUser(any(UserRequestDTO.class))).thenReturn(createUserCall);

    Call<RestResponse<Optional<UserInfo>>> getUserByIdCall = mock(Call.class);
    when(createUserCall.execute()).thenReturn(Response.success(new RestResponse<>(newUser)));
    when(userClient.getUserById(any())).thenReturn(getUserByIdCall);

    UserInfo returnedUser = signupServiceImpl.oAuthSignup(oAuthSignupDTO);

    verify(telemetryReporter, times(1))
        .sendTrackEvent(
            eq(SUCCEED_EVENT_NAME), eq(EMAIL), eq(ACCOUNT_ID), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
    verify(telemetryReporter, times(1)).sendIdentifyEvent(eq(EMAIL), any(), any());
    verify(executorService, times(1));
    assertThat(returnedUser.getEmail()).isEqualTo(newUser.getEmail());
  }

  @Test(expected = SignupException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testSignupWithInvalidEmail() {
    when(featureFlagService.isGlobalEnabled(FeatureName.NG_SIGNUP)).thenReturn(true);
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
    when(featureFlagService.isGlobalEnabled(FeatureName.NG_SIGNUP)).thenReturn(true);
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
    when(featureFlagService.isGlobalEnabled(FeatureName.NG_SIGNUP)).thenReturn(true);
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

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testResendEmailNotification() throws IOException {
    Mockito.when(SourcePrincipalContextBuilder.getSourcePrincipal())
        .thenReturn(new UserPrincipal("dummy", EMAIL, "dummy", ACCOUNT_ID));

    UserInfo user = UserInfo.builder().defaultAccountId(ACCOUNT_ID).email(EMAIL).build();
    Call<RestResponse<Optional<UserInfo>>> createUserCall = mock(Call.class);
    when(createUserCall.execute()).thenReturn(Response.success(new RestResponse<>(Optional.of(user))));
    when(userClient.getUserById(any())).thenReturn(createUserCall);
    SignupVerificationToken verificationToken = SignupVerificationToken.builder().userId("1").build();
    when(verificationTokenRepository.findByUserId("id")).thenReturn(Optional.of(verificationToken));
    when(verificationTokenRepository.save(any())).thenReturn(verificationToken);
    when(accountService.getBaseUrl(ACCOUNT_ID, NEXT_GEN_AUTH_URI)).thenReturn(NEXT_GEN_AUTH_URI);

    signupServiceImpl.resendVerificationEmail("id");
    verify(signupNotificationHelper, times(1))
        .sendSignupNotification(eq(user), eq(EmailType.VERIFY), any(),
            eq(NEXT_GEN_AUTH_URI + VERIFY_URL + "/" + verificationToken.getToken()));
    verify(verificationTokenRepository, times(1)).save(any());
    assertThat(verificationToken.getToken()).isNotNull();
  }

  @Test(expected = UnavailableFeatureException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testSignupWithFeatureFlagOff() {
    when(featureFlagService.isGlobalEnabled(FeatureName.NG_SIGNUP)).thenReturn(false);
    signupServiceImpl.signup(SignupDTO.builder().build(), null);
  }

  @Test(expected = UnavailableFeatureException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testOathSignupWithFeatureFlagOff() {
    when(featureFlagService.isGlobalEnabled(FeatureName.NG_SIGNUP)).thenReturn(false);
    signupServiceImpl.oAuthSignup(OAuthSignupDTO.builder().build());
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testVerifyToken() throws IOException {
    Call<RestResponse<Boolean>> changeUserVerifiedCall = mock(Call.class);
    when(changeUserVerifiedCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(userClient.changeUserEmailVerified(any())).thenReturn(changeUserVerifiedCall);
    when(verificationTokenRepository.findByToken("2"))
        .thenReturn(Optional.of(SignupVerificationToken.builder()
                                    .accountIdentifier(ACCOUNT_ID)
                                    .email(EMAIL)
                                    .userId("1")
                                    .token("2")
                                    .build()));
    VerifyTokenResponseDTO verifyTokenResponseDTO = signupServiceImpl.verifyToken("2");
    assertThat(verifyTokenResponseDTO.getAccountIdentifier()).isEqualTo(ACCOUNT_ID);
  }
}
