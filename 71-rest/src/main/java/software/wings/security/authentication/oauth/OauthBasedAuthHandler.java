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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.sso.OauthSettings;
import software.wings.security.authentication.AuthHandler;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.AuthenticationUtil;
import software.wings.service.impl.SSOSettingServiceImpl;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OauthBasedAuthHandler implements AuthHandler {
  @Inject AuthenticationUtil authenticationUtil;
  @Inject UserService userService;
  @Inject AuthService authService;
  @Inject MainConfiguration mainConfiguration;
  @Inject GithubClientImpl githubClient;
  @Inject BitbucketClient bitbucketClient;
  @Inject GitlabClient gitlabClient;
  @Inject LinkedinClientImpl linkedinClient;
  @Inject GoogleClientImpl googleClient;
  @Inject AzureClientImpl azureClient;
  @Inject SSOSettingServiceImpl ssoSettingService;
  @Inject UserServiceImpl userServiceImpl;

  static final Logger logger = LoggerFactory.getLogger(OauthBasedAuthHandler.class);

  @Override
  public AuthenticationMechanism getAuthenticationMechanism() {
    return AuthenticationMechanism.OAUTH;
  }

  @Override
  public User authenticate(String... credentials)
      throws URISyntaxException, InterruptedException, ExecutionException, IOException {
    if (credentials == null || credentials.length != 3) {
      throw new WingsException("Invalid arguments while authenticating using oauth");
    }
    final String code = credentials[0];
    final String state = credentials[1];
    final String domain = credentials[2];

    // for debugging purposes. Remove it.
    logger.info("Code is: [{}] and state is: [{}]", code, state);

    OauthClient oauthProvider = getOauthProvider(domain);
    final OauthUserInfo userInfo = oauthProvider.execute(code, state);
    final String email = userInfo.email;

    User user = null;
    try {
      user = authenticationUtil.getUser(email);
      matchOauthDomainAndApplyEmailFilter(user, oauthProvider);

      // User can't ne null. If the user is not found, we expect a exception.
      AuthenticationMechanism userAuthMechanism = userService.getAuthenticationMechanism(user);
      if (!userAuthMechanism.equals(AuthenticationMechanism.OAUTH)) {
        // if the User is from freemium and is trying to signup,
        // he should get a mail saying that this is not his auth mechanism.
        sendTrialSignupCompleteMailForFreeUsers(user);

        logger.error(String.format("User [{}] tried to login using oauth while his authentication mechanism was: [{}]"),
            user.getEmail(), userAuthMechanism);
        throw new WingsException(ErrorCode.INCORRECT_SIGN_IN_MECHANISM);
      }
    } catch (WingsException we) {
      if (ErrorCode.USER_DOES_NOT_EXIST.equals(we.getCode())) {
        // Coming in this flow means that the user's email is not in the harness system.
        // Create an account for the user and then log him in.

        user = userService.completeOauthSignup(userInfo, oauthProvider);
      } else {
        throw we;
      }
    } catch (Exception ex) {
      logger.error(String.format("Failed to login via OauthBasedAuthHandler, email was %s", email), ex);
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, WingsException.USER);
    }
    return user;
  }

  private void sendTrialSignupCompleteMailForFreeUsers(User user) {
    if (mainConfiguration.isTrialRegistrationAllowed()) {
      UserInvite userInvite = userServiceImpl.getUserInviteByEmail(user.getEmail());
      userServiceImpl.sendTrialSignupCompletedEmail(userInvite);
    }
  }

  private void matchOauthDomainAndApplyEmailFilter(final User user, final OauthClient oauthClient) {
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

    logger.info("Applying email filter for user: {} and filter {}", user.getEmail(), filter);
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

  private OauthClient getOauthProvider(final String domain) throws URISyntaxException {
    switch (valueOf(domain)) {
      case github:
        return githubClient;
      case linkedin:
        return linkedinClient;
      case google:
        return googleClient;
      case azure:
        return azureClient;
      case bitbucket:
        return bitbucketClient;
      case gitlab:
        return gitlabClient;
      default:
        throw new WingsException(
            String.format("Host URI did not match to any providers. Received host :[%s]", getHost(domain)));
    }
  }

  private String getHost(String domain) throws URISyntaxException {
    return new URI(domain).getHost();
  }
}
