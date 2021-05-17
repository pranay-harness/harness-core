package io.harness.signup.services.impl;

import static io.harness.annotations.dev.HarnessTeam.GTM;
import static io.harness.remote.client.RestClientUtils.getResponse;

import static org.mindrot.jbcrypt.BCrypt.hashpw;

import io.harness.account.services.AccountService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.authenticationservice.recaptcha.ReCaptchaVerifier;
import io.harness.exception.SignupException;
import io.harness.exception.UserAlreadyPresentException;
import io.harness.exception.WeakPasswordException;
import io.harness.exception.WingsException;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserRequestDTO;
import io.harness.notification.templates.PredefinedTemplate;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;
import io.harness.security.dto.UserPrincipal;
import io.harness.signup.data.UtmInfo;
import io.harness.signup.dto.OAuthSignupDTO;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.notification.EmailType;
import io.harness.signup.notification.SignupNotificationHelper;
import io.harness.signup.services.SignupService;
import io.harness.signup.validator.SignupValidator;
import io.harness.telemetry.Category;
import io.harness.telemetry.Destination;
import io.harness.telemetry.TelemetryReporter;
import io.harness.user.remote.UserClient;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@OwnedBy(GTM)
public class SignupServiceImpl implements SignupService {
  private AccountService accountService;
  private UserClient userClient;
  private SignupValidator signupValidator;
  private ReCaptchaVerifier reCaptchaVerifier;
  private final TelemetryReporter telemetryReporter;
  private final SignupNotificationHelper signupNotificationHelper;
  @Named("NGSignupNotification") private final ExecutorService executorService;

  public static final String FAILED_EVENT_NAME = "Signup attempt failed";
  public static final String SUCCEED_EVENT_NAME = "Signup succeed";

  @Override
  public UserInfo signup(SignupDTO dto, String captchaToken) throws WingsException {
    verifyReCaptcha(dto, captchaToken);
    verifyEmailAndPassword(dto);

    AccountDTO account = createAccount(dto);
    UserInfo user = createUser(dto, account);
    sendSucceedTelemetryEvent(dto.getEmail(), dto.getUtmInfo(), account, user);
    executorService.submit(()
                               -> signupNotificationHelper.sendSignupNotification(
                                   user, EmailType.VERIFY, PredefinedTemplate.EMAIL_VERIFY.getIdentifier()));
    return user;
  }

  private AccountDTO createAccount(SignupDTO dto) {
    try {
      return accountService.createAccount(dto);
    } catch (Exception e) {
      sendFailedTelemetryEvent(dto.getEmail(), dto.getUtmInfo(), e, null, "Account creation");
      throw e;
    }
  }

  private void verifyReCaptcha(SignupDTO dto, String captchaToken) {
    try {
      reCaptchaVerifier.verifyInvisibleCaptcha(captchaToken);
    } catch (Exception e) {
      sendFailedTelemetryEvent(dto.getEmail(), dto.getUtmInfo(), e, null, "ReCaptcha verification");
      throw e;
    }
  }

  private void verifyEmailAndPassword(SignupDTO dto) {
    try {
      signupValidator.validateSignup(dto);
    } catch (SignupException | UserAlreadyPresentException e) {
      sendFailedTelemetryEvent(dto.getEmail(), dto.getUtmInfo(), e, null, "Email validation");
      throw e;
    } catch (WeakPasswordException we) {
      sendFailedTelemetryEvent(dto.getEmail(), dto.getUtmInfo(), we, null, "Password validation");
      throw we;
    }
  }

  @Override
  public UserInfo oAuthSignup(OAuthSignupDTO dto) {
    try {
      signupValidator.validateEmail(dto.getEmail());
    } catch (SignupException | UserAlreadyPresentException e) {
      sendFailedTelemetryEvent(dto.getEmail(), dto.getUtmInfo(), e, null, "Email validation");
      throw e;
    }

    SignupDTO signupDTO = SignupDTO.builder().email(dto.getEmail()).utmInfo(dto.getUtmInfo()).build();
    AccountDTO account = createAccount(signupDTO);
    UserInfo oAuthUser = createOAuthUser(dto, account);
    sendSucceedTelemetryEvent(dto.getEmail(), dto.getUtmInfo(), account, oAuthUser);

    executorService.submit(()
                               -> signupNotificationHelper.sendSignupNotification(oAuthUser, EmailType.CONFIRM,
                                   PredefinedTemplate.SIGNUP_CONFIRMATION.getIdentifier()));
    return oAuthUser;
  }

  @Override
  public void resendVerificationEmail(String userId) {
    Optional<UserInfo> response = getResponse(userClient.getUserById(userId));
    if (!response.isPresent()) {
      throw new WingsException(String.format("UserId [%s] doesn't exist", userId));
    }
    UserInfo userInfo = response.get();
    if (userInfo.isEmailVerified()) {
      throw new WingsException(String.format("Email has been verified for userId [%s]", userId));
    }

    // Verify the request userId is current user
    if (!verifyCurrentUserEmail(userInfo.getEmail())) {
      throw new WingsException(String.format("Invalid resend request for userId [%s]", userId));
    }

    signupNotificationHelper.sendSignupNotification(
        userInfo, EmailType.VERIFY, PredefinedTemplate.EMAIL_VERIFY.getIdentifier());
  }

  private boolean verifyCurrentUserEmail(String email) {
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      String userEmail = ((UserPrincipal) (SourcePrincipalContextBuilder.getSourcePrincipal())).getEmail();
      return email.equals(userEmail);
    }
    return false;
  }

  private UserInfo createUser(SignupDTO signupDTO, AccountDTO account) {
    try {
      String passwordHash = hashpw(signupDTO.getPassword(), BCrypt.gensalt());
      List<AccountDTO> accountList = new ArrayList<>();
      accountList.add(account);

      String name = account.getName();

      UserRequestDTO userRequest = UserRequestDTO.builder()
                                       .email(signupDTO.getEmail())
                                       .name(name)
                                       .passwordHash(passwordHash)
                                       .accountName(account.getName())
                                       .companyName(account.getCompanyName())
                                       .accounts(accountList)
                                       .emailVerified(false)
                                       .defaultAccountId(account.getIdentifier())
                                       .build();
      return getResponse(userClient.createNewUser(userRequest));
    } catch (Exception e) {
      sendFailedTelemetryEvent(signupDTO.getEmail(), signupDTO.getUtmInfo(), e, account, "User creation");
      throw e;
    }
  }

  private void sendFailedTelemetryEvent(
      String email, UtmInfo utmInfo, Exception e, AccountDTO accountDTO, String failedAt) {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("reason", e.getMessage());
    properties.put("failedAt", failedAt);
    addUtmInfoToProperties(utmInfo, properties);

    if (accountDTO != null) {
      properties.put("company", accountDTO.getCompanyName());
      telemetryReporter.sendTrackEvent(FAILED_EVENT_NAME, email, accountDTO.getIdentifier(), properties,
          ImmutableMap.<Destination, Boolean>builder().put(Destination.SALESFORCE, true).build(), Category.SIGN_UP);
    } else {
      telemetryReporter.sendTrackEvent(FAILED_EVENT_NAME, email, null, properties,
          ImmutableMap.<Destination, Boolean>builder().put(Destination.SALESFORCE, true).build(), Category.SIGN_UP);
    }
  }

  private void sendSucceedTelemetryEvent(String email, UtmInfo utmInfo, AccountDTO accountDTO, UserInfo userInfo) {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("company", accountDTO.getCompanyName());
    properties.put("email", userInfo.getEmail());
    properties.put("name", userInfo.getName());
    properties.put("id", userInfo.getUuid());
    addUtmInfoToProperties(utmInfo, properties);
    telemetryReporter.sendIdentifyEvent(userInfo.getEmail(), properties,
        ImmutableMap.<Destination, Boolean>builder().put(Destination.SALESFORCE, true).build());

    telemetryReporter.sendTrackEvent(SUCCEED_EVENT_NAME, email, accountDTO.getIdentifier(), properties,
        ImmutableMap.<Destination, Boolean>builder().put(Destination.SALESFORCE, true).build(), Category.SIGN_UP);
  }

  private void addUtmInfoToProperties(UtmInfo utmInfo, HashMap<String, Object> properties) {
    if (utmInfo != null) {
      properties.put("utmSource", utmInfo.getUtmSource());
      properties.put("utmContent", utmInfo.getUtmContent());
      properties.put("utmMedium", utmInfo.getUtmMedium());
      properties.put("utmTerm", utmInfo.getUtmTerm());
      properties.put("utmCampaign", utmInfo.getUtmCampaign());
    }
  }

  private UserInfo createOAuthUser(OAuthSignupDTO oAuthSignupDTO, AccountDTO account) {
    try {
      UserRequestDTO userRequest = UserRequestDTO.builder()
                                       .email(oAuthSignupDTO.getEmail())
                                       .name(oAuthSignupDTO.getName())
                                       .accountName(account.getName())
                                       .companyName(account.getCompanyName())
                                       .accounts(Arrays.asList(account))
                                       .emailVerified(true)
                                       .defaultAccountId(account.getIdentifier())
                                       .build();

      return getResponse(userClient.createNewOAuthUser(userRequest));
    } catch (Exception e) {
      sendFailedTelemetryEvent(
          oAuthSignupDTO.getEmail(), oAuthSignupDTO.getUtmInfo(), e, account, "OAuth user creation");
      throw e;
    }
  }
}
