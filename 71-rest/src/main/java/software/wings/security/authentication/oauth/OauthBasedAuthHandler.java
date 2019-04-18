package software.wings.security.authentication.oauth;

import static software.wings.security.authentication.oauth.OauthOptions.SupportedOauthProviders.valueOf;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.utils.Strings;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.app.MainConfiguration;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.sso.OauthSettings;
import software.wings.security.authentication.AuthHandler;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.AuthenticationResponse;
import software.wings.security.authentication.AuthenticationUtil;
import software.wings.security.authentication.OauthAuthenticationResponse;
import software.wings.service.impl.SSOSettingServiceImpl;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * This class authenticates user using oauth flow.
 * It calls the corresponding OauthProvider for getting the email and once it receives that,
 * applies the authentication mechanism filters to ensure that user is logging using the
 * correct auth mechanism.
 */
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class OauthBasedAuthHandler implements AuthHandler {
  @Inject AuthenticationUtil authenticationUtil;
  @Inject UserService userService;
  @Inject MainConfiguration mainConfiguration;
  @Inject SSOSettingServiceImpl ssoSettingService;
  @Inject UserServiceImpl userServiceImpl;
  @Inject OauthOptions oauthOptions;

  @Override
  public AuthenticationMechanism getAuthenticationMechanism() {
    return AuthenticationMechanism.OAUTH;
  }

  /**
   * @param credentials - Array of string having 3 values.
   *  1) authorization_code - Special code which is to be used in further communication with the service provider.
   *  2) state - this is special random jwt token code service provider sends when redirecting to harness as get param.
   *             This code is generated from harness side and the auth provider sends it back to confirm that the
   * request initiated from Harness and prevents chances of CSRF attack. 3) provider - The oauth provider sending the
   * request
   * @return the user associated with the email. If the user does not exists in harness, create and return for freemium
   *     cluster.
   */
  @Override
  public AuthenticationResponse authenticate(String... credentials)
      throws InterruptedException, ExecutionException, IOException {
    if (credentials == null || credentials.length != 3) {
      throw new WingsException("Invalid arguments while authenticating using oauth");
    }
    final String code = credentials[0];
    final String state = credentials[1];
    final String domain = credentials[2];

    OauthClient oauthProvider = getOauthProvider(domain);
    final OauthUserInfo userInfo = oauthProvider.execute(code, state);

    User user = null;
    try {
      user = authenticationUtil.getUserOrReturnNullIfUserDoesNotExists(userInfo.getEmail());

      // if the email doesn't exists in harness system, sign him up.
      if (null == user) {
        return OauthAuthenticationResponse.builder()
            .oauthUserInfo(userInfo)
            .userFoundInDB(false)
            .oauthClient(oauthProvider)
            .build();
      } else {
        verifyAuthMechanismOfUser(user, oauthProvider);
        return new AuthenticationResponse(user);
      }
    } catch (Exception ex) {
      logger.error(String.format("Failed to login via OauthBasedAuthHandler, email was %s", userInfo.getEmail()), ex);
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, WingsException.USER);
    }
  }

  private void verifyAuthMechanismOfUser(User user, OauthClient oauthProvider) {
    matchOauthProviderAndApplyEmailFilter(user, oauthProvider);
    verifyAccountLevelAuthMechanismEqualsOauth(user);
  }

  private void verifyAccountLevelAuthMechanismEqualsOauth(User user) {
    AuthenticationMechanism userAuthMechanism = userService.getAuthenticationMechanism(user);
    if (!userAuthMechanism.equals(AuthenticationMechanism.OAUTH)) {
      // Freemium user who has already signed up should get a mail saying his signup is complete and ask him to login on
      // harness website.
      sendTrialSignupCompleteMailForFreeUsers(user);

      logger.error(
          String.format("User [{}] tried to login using OauthMechanism while his authentication mechanism was: [{}]"),
          user.getEmail(), userAuthMechanism);
      throw new WingsException(ErrorCode.INCORRECT_SIGN_IN_MECHANISM);
    }
  }

  private void sendTrialSignupCompleteMailForFreeUsers(User user) {
    if (mainConfiguration.isTrialRegistrationAllowed()) {
      UserInvite userInvite = userServiceImpl.getUserInviteByEmail(user.getEmail());
      userServiceImpl.sendTrialSignupCompletedEmail(userInvite);
    }
  }

  private void matchOauthProviderAndApplyEmailFilter(final User user, final OauthClient oauthClient) {
    OauthSettings oauthSettings =
        ssoSettingService.getOauthSettingsByAccountId(authenticationUtil.getPrimaryAccount(user).getUuid());
    if (oauthSettings == null) {
      return;
    }
    matchOauthProviderAndAuthMechanism(oauthClient.getName(), oauthSettings.getDisplayName());
    applyEmailFilter(user, oauthSettings);
  }

  private void applyEmailFilter(User user, OauthSettings oauthSettings) {
    String filter = oauthSettings.getFilter();
    if (Strings.isNullOrBlank(filter)) {
      return;
    }

    String[] filters = filter.split(",");
    filters = StringUtils.stripAll(filters);
    String email = user.getEmail();

    String domain = email.substring(email.indexOf('@') + 1);
    if (!Arrays.asList(filters).contains(domain)) {
      logger.error(String.format("Domain filter was: [%s] while the email was: [%s]", Arrays.asList(filters), email));
      throw new WingsException("Domain name filter failed. Please contact your Account Administrator");
    }
  }

  private void matchOauthProviderAndAuthMechanism(String oauthProvider, String oauthMechInDB) {
    if (!oauthMechInDB.equals(oauthProvider)) {
      logger.error("Mismatch in the oauth provider received {} and oauth provided in access settings {}", oauthProvider,
          oauthMechInDB);
      throw new InvalidRequestException("Oauth provider mismatch.");
    }
  }

  private OauthClient getOauthProvider(final String domain) {
    return oauthOptions.getOauthProvider(valueOf(domain));
  }
}
