package software.wings.signup;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_EMAIL;
import static io.harness.eraro.ErrorCode.PASSWORD_STRENGTH_CHECK_FAILED;
import static io.harness.exception.WingsException.USER;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.http.client.utils.URIBuilder;
import software.wings.app.MainConfiguration;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInvite.UserInviteKeys;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WeakPasswordException;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.SignupService;
import software.wings.service.intfc.signup.SignupException;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class SignupServiceImpl implements SignupService {
  @Inject EmailNotificationService emailNotificationService;
  @Inject MainConfiguration configuration;
  @Inject WingsPersistence wingsPersistence;
  @Inject BlackListedDomainChecker blackListedDomainChecker;
  @Inject MainConfiguration mainConfiguration;
  @Inject private SubdomainUrlHelperIntfc subdomainUrlHelper;
  @Inject AccountService accountService;
  @Inject PwnedPasswordChecker pwnedPasswordChecker;

  private static final String TRIAL_SIGNUP_COMPLETED_TEMPLATE_NAME = "trial_signup_completed";
  private static final String SETUP_PASSWORD_FOR_SIGNUP = "setup_password_for_signup";

  private List<Character> ILLEGAL_CHARACTERS = Collections.unmodifiableList(Arrays.asList(
      '$', '&', ',', '/', ':', ';', '=', '?', '<', '>', '#', '{', '}', '|', '^', '~', '(', ')', ']', '`', '\'', '\"'));

  @Override
  public void sendTrialSignupCompletedEmail(UserInvite userInvite) {
    try {
      Map<String, String> templateModel = getTrialSignupCompletedTemplateModel(userInvite);
      sendEmail(userInvite, TRIAL_SIGNUP_COMPLETED_TEMPLATE_NAME, templateModel);
    } catch (URISyntaxException e) {
      logger.error("Trial sign-up completed email couldn't be sent ", e);
    }
  }

  private Map<String, String> getTrialSignupCompletedTemplateModel(UserInvite userInvite) throws URISyntaxException {
    Map<String, String> model = new HashMap<>();
    String loginUrl = buildAbsoluteUrl("/login", userInvite);
    model.put("name", userInvite.getEmail());
    model.put("url", loginUrl);
    return model;
  }

  private String buildAbsoluteUrl(String fragment, UserInvite userInvite) throws URISyntaxException {
    String baseUrl = subdomainUrlHelper.getPortalBaseUrl(userInvite.getAccountId());
    URIBuilder uriBuilder = new URIBuilder(baseUrl);
    uriBuilder.setFragment(fragment);
    return uriBuilder.toString();
  }

  @Override
  public void sendEmail(UserInvite userInvite, String templateName, Map<String, String> templateModel) {
    List<String> toList = new ArrayList<>();
    toList.add(userInvite.getEmail());
    EmailData emailData =
        EmailData.builder().to(toList).templateName(templateName).templateModel(templateModel).build();

    emailData.setCc(Collections.emptyList());
    emailData.setRetries(2);
    emailData.setAccountId(userInvite.getAccountId());

    emailNotificationService.send(emailData);
  }

  @Override
  public UserInvite getUserInviteByEmail(String email) {
    UserInvite userInvite = null;
    if (isNotEmpty(email)) {
      userInvite = wingsPersistence.createQuery(UserInvite.class).filter(UserInviteKeys.email, email).get();
    }
    return userInvite;
  }

  @Override
  public void validateEmail(String email) {
    // Only validate if the email address is valid. Won't check if the email has been registered already.
    checkIfEmailIsValid(email);

    if (containsIllegalCharacters(email)) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "The email used for trial registration contains illegal characters.");
    }
    blackListedDomainChecker.check(email);
  }

  @Override
  public void validateCluster() {
    if (!configuration.isTrialRegistrationAllowed()) {
      throw new SignupException("Signup not allowed in this cluster");
    }
  }

  private boolean containsIllegalCharacters(String email) {
    for (Character illegalChar : ILLEGAL_CHARACTERS) {
      if (email.indexOf(illegalChar) >= 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void checkIfEmailIsValid(String email) {
    if (isBlank(email)) {
      throw new WingsException(INVALID_EMAIL, USER).addParam("email", email);
    }

    final String emailAddress = email.trim();
    if (!EmailValidator.getInstance().isValid(emailAddress)) {
      throw new WingsException(INVALID_EMAIL, USER).addParam("email", emailAddress);
    }
  }

  @Override
  public void sendPasswordSetupMailForSignup(UserInvite userInvite) {
    String jwtPasswordSecret = configuration.getPortal().getJwtPasswordSecret();
    try {
      String token = createSignupTokeFromSecret(jwtPasswordSecret, userInvite.getEmail(), 30);
      String resetPasswordUrl = getResetPasswordUrl(token, userInvite);
      sendMail(userInvite, resetPasswordUrl, SETUP_PASSWORD_FOR_SIGNUP);
    } catch (URISyntaxException | UnsupportedEncodingException e) {
      logger.error("Password setup mail for signup could't be sent", e);
    }
  }

  @Override
  public String createSignupTokeFromSecret(String jwtPasswordSecret, String email, int expireAfterDays)
      throws UnsupportedEncodingException {
    Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
    return JWT.create()
        .withIssuer("Harness Inc")
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + (long) expireAfterDays * 24 * 60 * 60 * 1000)) // 24 hrs
        .withClaim("email", email)
        .sign(algorithm);
  }

  private void sendMail(UserInvite userInvite, String resetPasswordUrl, String resetPasswordTemplateName) {
    Map<String, String> templateModel = new HashMap<>();
    templateModel.put("url", resetPasswordUrl);
    templateModel.put("name", userInvite.getName());
    templateModel.put("companyName", userInvite.getCompanyName());
    List<String> toList = new ArrayList<>();
    toList.add(userInvite.getEmail());
    EmailData emailData =
        EmailData.builder().to(toList).templateName(resetPasswordTemplateName).templateModel(templateModel).build();
    emailData.setCc(Collections.emptyList());
    emailData.setRetries(2);

    emailNotificationService.send(emailData);
  }

  private String getResetPasswordUrl(String token, UserInvite userInvite) throws URISyntaxException {
    // always the call should go to the free cluster because a trial account will be created.
    String mode = "?mode=signup";
    return buildAbsoluteUrl("/complete-signup/" + token + mode, userInvite);
  }

  @Override
  public void validatePassword(char[] password) {
    if (isEmpty(password)) {
      throw new WeakPasswordException(
          "Password cannot be empty.", null, PASSWORD_STRENGTH_CHECK_FAILED, Level.ERROR, USER, null);
    }

    if (password.length < 8) {
      throw new WeakPasswordException(
          "Password should at least be 8 characters.", null, PASSWORD_STRENGTH_CHECK_FAILED, Level.ERROR, USER, null);
    }

    if (password.length > 64) {
      throw new WeakPasswordException("Password should be less than or equal to 64 characters.", null,
          PASSWORD_STRENGTH_CHECK_FAILED, Level.ERROR, USER, null);
    }

    if (!mainConfiguration.isPwnedPasswordsAllowed()) {
      boolean isPasswordPwned = false;

      try {
        isPasswordPwned = pwnedPasswordChecker.checkIfPwned(password);
      } catch (Exception e) {
        logger.error("Received exception while checking for pwned passwords. Logging and ignoring the check", e);
      }

      if (isPasswordPwned) {
        throw new WeakPasswordException(
            "The password you entered has been flagged as vulnerable. To ensure security of your account, please enter a different password.",
            null, PASSWORD_STRENGTH_CHECK_FAILED, Level.ERROR, USER, null);
      }
    }
  }

  @Override
  public String getEmail(String jwtToken) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(mainConfiguration.getPortal().getJwtPasswordSecret());
      JWTVerifier verifier = JWT.require(algorithm).withIssuer("Harness Inc").build();
      verifier.verify(jwtToken);
      JWT decode = JWT.decode(jwtToken);
      return decode.getClaim("email").asString();
    } catch (UnsupportedEncodingException exception) {
      logger.error("Could not decode token for signup: {}", jwtToken);
      throw new SignupException("Invalid signup token. Please signup again");
    } catch (JWTVerificationException exception) {
      logger.error("Signup token {} has expired", jwtToken);
      throw new SignupException("Invalid signup token. Please signup again");
    }
  }

  @Override
  public void checkIfUserInviteIsValid(UserInvite userInvite, String email) {
    if (userInvite == null) {
      logger.info("No invite found in db for for email: {}", email);
      throw new SignupException(String.format("Can not process signup for email: %s", email));
    } else if (userInvite.isCompleted()) {
      throw new SignupException("User invite has already been completed. Please login");
    }
  }
}
