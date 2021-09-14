/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.signup.services;

import static io.harness.annotations.dev.HarnessTeam.GTM;
import static io.harness.rule.OwnerRule.NATHAN;
import static io.harness.rule.OwnerRule.ZHUO;
import static io.harness.signup.services.impl.SignupServiceImpl.FAILED_EVENT_NAME;
import static io.harness.signup.services.impl.SignupServiceImpl.SUCCEED_SIGNUP_INVITE_NAME;

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
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.account.services.AccountService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.authenticationservice.recaptcha.ReCaptchaVerifier;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SignupException;
import io.harness.exception.WingsException;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserRequestDTO;
import io.harness.repositories.SignupVerificationTokenRepository;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.signup.dto.OAuthSignupDTO;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.dto.SignupInviteDTO;
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
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(GTM)
@PrepareForTest(SourcePrincipalContextBuilder.class)
public class SignupServiceImplTest extends CategoryTest {
  @InjectMocks SignupServiceImpl signupServiceImpl;
  @Mock SignupValidator signupValidator;
  @Mock AccountService accountService;
  @Mock UserClient userClient;
  @Mock AccessControlClient accessControlClient;
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
  private static final String NEXT_GEN_PORATL = "http://localhost:8181/";

  @Before
  public void setup() throws IllegalAccessException {
    initMocks(this);
    signupServiceImpl = new SignupServiceImpl(accountService, userClient, signupValidator, reCaptchaVerifier,
        telemetryReporter, signupNotificationHelper, verificationTokenRepository, executorService, accessControlClient);
  }

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
    verify(telemetryReporter, times(1)).sendIdentifyEvent(eq(EMAIL), any(), any());
    verify(executorService, times(1));
    assertThat(returnedUser.getEmail()).isEqualTo(newUser.getEmail());
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCreateSignupInvite() throws IOException {
    SignupDTO signupDTO = SignupDTO.builder().email(EMAIL).password(PASSWORD).intent("CI").build();
    doNothing().when(signupValidator).validateSignup(any(SignupDTO.class));

    Call<RestResponse<SignupInviteDTO>> createNewSignupInviteCall = mock(Call.class);
    when(createNewSignupInviteCall.execute())
        .thenReturn(Response.success(new RestResponse<>(SignupInviteDTO.builder().build())));
    when(userClient.createNewSignupInvite(any(SignupInviteDTO.class))).thenReturn(createNewSignupInviteCall);

    boolean result = signupServiceImpl.createSignupInvite(signupDTO, null);

    verify(reCaptchaVerifier, times(1)).verifyInvisibleCaptcha(anyString());
    verify(telemetryReporter, times(1))
        .sendTrackEvent(eq(SUCCEED_SIGNUP_INVITE_NAME), eq(EMAIL), any(), any(), any(), any());
    verify(executorService, times(1));
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCompleteSignupInvite() throws IOException {
    Call<RestResponse<UserInfo>> completeSignupInviteCall = mock(Call.class);
    when(completeSignupInviteCall.execute())
        .thenReturn(Response.success(
            new RestResponse<>(UserInfo.builder().email(EMAIL).defaultAccountId(ACCOUNT_ID).intent("CI").build())));
    when(userClient.completeSignupInvite(any())).thenReturn(completeSignupInviteCall);

    SignupVerificationToken verificationToken =
        SignupVerificationToken.builder().email(EMAIL).validUntil(Long.MAX_VALUE).build();
    when(verificationTokenRepository.findByToken("token")).thenReturn(Optional.of(verificationToken));
    when(accessControlClient.hasAccess(any(), any(), any())).thenReturn(true);
    UserInfo userInfo = signupServiceImpl.completeSignupInvite("token");

    verify(telemetryReporter, times(1)).sendIdentifyEvent(eq(EMAIL), any(), any());
    verify(executorService, times(1));
    assertThat(userInfo.getIntent()).isEqualTo("CI");
    assertThat(userInfo.getEmail()).isEqualTo(EMAIL);
    assertThat(userInfo.getDefaultAccountId()).isEqualTo(ACCOUNT_ID);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCompleteSignupInviteWithInvalidToken() throws IOException {
    when(verificationTokenRepository.findByToken("token")).thenReturn(Optional.ofNullable(null));
    signupServiceImpl.completeSignupInvite("token");
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

    Call<RestResponse<Optional<UserInfo>>> getUserByIdCall = mock(Call.class);
    when(createUserCall.execute()).thenReturn(Response.success(new RestResponse<>(newUser)));
    when(userClient.getUserById(any())).thenReturn(getUserByIdCall);
    when(accessControlClient.hasAccess(any(), any(), any())).thenReturn(true);

    UserInfo returnedUser = signupServiceImpl.oAuthSignup(oAuthSignupDTO);

    verify(telemetryReporter, times(1)).sendIdentifyEvent(eq(EMAIL), any(), any());
    verify(executorService, times(1));
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

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testResendEmailNotification() throws IOException, URISyntaxException {
    SignupInviteDTO signupInviteDTO =
        SignupInviteDTO.builder().email(EMAIL).completed(false).createdFromNG(true).build();
    Call<RestResponse<SignupInviteDTO>> getSignupInviteCall = mock(Call.class);
    when(getSignupInviteCall.execute()).thenReturn(Response.success(new RestResponse<>(signupInviteDTO)));
    when(userClient.getSignupInvite(EMAIL)).thenReturn(getSignupInviteCall);
    SignupVerificationToken verificationToken =
        SignupVerificationToken.builder().email(EMAIL).validUntil(Long.MAX_VALUE).token("123").build();
    when(verificationTokenRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verificationToken));
    when(verificationTokenRepository.save(any())).thenReturn(verificationToken);
    when(accountService.getBaseUrl(any())).thenReturn(NEXT_GEN_PORATL);

    signupServiceImpl.resendVerificationEmail(EMAIL);
    verify(signupNotificationHelper, times(1))
        .sendSignupNotification(any(), eq(EmailType.VERIFY), any(),
            eq(NEXT_GEN_PORATL + "auth/#/" + VERIFY_URL + "/" + verificationToken.getToken() + "?email=" + EMAIL));
    verify(verificationTokenRepository, times(1)).save(any());
    assertThat(verificationToken.getToken()).isNotNull();
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
