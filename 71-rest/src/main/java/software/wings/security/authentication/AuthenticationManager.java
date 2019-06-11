package software.wings.security.authentication;

import static io.harness.data.encoding.EncodingUtils.decodeBase64ToString;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.EMAIL_NOT_VERIFIED;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.eraro.ErrorCode.USER_DISABLED;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static io.harness.eraro.ErrorCode.USER_NOT_AUTHORIZED;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.FeatureName.LOGIN_PROMPT_WHEN_NO_USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AuthToken;
import software.wings.beans.FeatureName;
import software.wings.beans.User;
import software.wings.licensing.LicenseService;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.authentication.LoginTypeResponse.LoginTypeResponseBuilder;
import software.wings.security.authentication.oauth.OauthBasedAuthHandler;
import software.wings.security.authentication.oauth.OauthOptions;
import software.wings.security.saml.SSORequest;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.core.Response;

@Singleton
@Slf4j
public class AuthenticationManager {
  @Inject private PasswordBasedAuthHandler passwordBasedAuthHandler;
  @Inject private SamlBasedAuthHandler samlBasedAuthHandler;
  @Inject private LdapBasedAuthHandler ldapBasedAuthHandler;
  @Inject private AuthenticationUtils authenticationUtils;
  @Inject private SamlClientService samlClientService;
  @Inject private MainConfiguration configuration;
  @Inject private UserService userService;
  @Inject private AccountService accountService;
  @Inject private AuthService authService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private OauthBasedAuthHandler oauthBasedAuthHandler;
  @Inject private OauthOptions oauthOptions;
  @Inject private SSOSettingService ssoSettingService;

  @Inject private LicenseService licenseService;

  public AuthHandler getAuthHandler(AuthenticationMechanism mechanism) {
    switch (mechanism) {
      case SAML:
        return samlBasedAuthHandler;
      case LDAP:
        return ldapBasedAuthHandler;
      case OAUTH:
        return oauthBasedAuthHandler;
      default:
        return passwordBasedAuthHandler;
    }
  }

  private AuthenticationMechanism getAuthenticationMechanism(User user, String accountId) {
    if (user.isDisabled()) {
      throw new WingsException(USER_DISABLED, USER);
    }
    AuthenticationMechanism authenticationMechanism;
    if (isNotEmpty(accountId)) {
      // First check if the user is associated with the account.
      if (!userService.isUserAssignedToAccount(user, accountId)) {
        throw new InvalidRequestException("User is not assigned to account", USER);
      }
      // If account is specified, using the specified account's auth mechanism
      Account account = accountService.get(accountId);
      authenticationMechanism = account.getAuthenticationMechanism();
    } else {
      /*
       * Choose the first account as primary account, use its auth mechanism for login purpose if the user is
       * associated with multiple accounts. As the UI will always pick the first account to start with after the logged
       * in user is having a list of associated accounts.
       */
      String defaultAccountId = user.getDefaultAccountId();
      Optional<Account> account =
          user.getAccounts().stream().filter(acct -> Objects.equals(defaultAccountId, acct.getUuid())).findFirst();
      if (account.isPresent()) {
        authenticationMechanism = account.get().getAuthenticationMechanism();
      } else {
        authenticationMechanism = user.getAccounts().get(0).getAuthenticationMechanism();
      }
    }

    if (authenticationMechanism == null) {
      authenticationMechanism = AuthenticationMechanism.USER_PASSWORD;
    }
    return authenticationMechanism;
  }

  public AuthenticationMechanism getAuthenticationMechanism(String userName) {
    return getAuthenticationMechanism(authenticationUtils.getUser(userName, USER), null);
  }

  public LoginTypeResponse getLoginTypeResponse(String userName) {
    return getLoginTypeResponse(userName, null);
  }

  public LoginTypeResponse getLoginTypeResponse(String userName, String accountId) {
    LoginTypeResponseBuilder builder = LoginTypeResponse.builder();

    /*
     * To prevent possibility of user enumeration (https://harness.atlassian.net/browse/HAR-7188),
     * instead of throwing the USER_DOES_NOT_EXIST exception, send USER_PASSWORD as the login mechanism.
     * The next page throws INVALID_CREDENTIAL exception in case of wrong userId/password which doesn't reveals any
     * information.
     */
    try {
      User user = authenticationUtils.getUser(userName, USER);
      AuthenticationMechanism authenticationMechanism = getAuthenticationMechanism(user, accountId);

      builder.isOauthEnabled(userService.isOauthEnabled(user));

      SSORequest ssoRequest;
      switch (authenticationMechanism) {
        case USER_PASSWORD:
          if (!user.isEmailVerified()) {
            // HAR-7984: Return 401 http code if user email not verified yet.
            throw new WingsException(EMAIL_NOT_VERIFIED, USER);
          }
          if (null != ssoSettingService.getOauthSettingsByAccountId(user.getDefaultAccountId())) {
            builder.SSORequest(oauthOptions.oauthProviderRedirectionUrl(user));
            break;
          }
          break;
        case SAML:
          ssoRequest = samlClientService.generateSamlRequest(user);
          builder.SSORequest(ssoRequest);
          break;
        case OAUTH:
          builder.SSORequest(oauthOptions.oauthProviderRedirectionUrl(user));
          break;
        case LDAP: // No need to build anything extra for the response.
        default:
          // Nothing to do by default
      }
      return builder.authenticationMechanism(authenticationMechanism).build();
    } catch (WingsException we) {
      if (featureFlagService.isEnabled(LOGIN_PROMPT_WHEN_NO_USER, GLOBAL_ACCOUNT_ID)) {
        logger.warn(we.getMessage(), we);
        return builder.authenticationMechanism(AuthenticationMechanism.USER_PASSWORD).build();
      } else {
        throw we;
      }
    }
  }

  public User switchAccount(String bearerToken, String accountId) {
    AuthToken authToken = authService.validateToken(bearerToken);
    User user = authToken.getUser();
    user.setLastAccountId(accountId);
    return authService.generateBearerTokenForUser(user);
  }

  /**
   * PLEASE DON'T CALL THESE API DIRECTLY if the call is not from identity service!
   *
   * This API is only for Identity Service to login user directly because identity service have already
   * been authenticated the user through OAUTH etc auth mechanism and need a trusted explicit login from
   * manager.
   */
  public User loginUserForIdentityService(String email) {
    User user = userService.getUserByEmail(email);
    // Null check just in case identity service might accidentally forwarded wrong user to this cluster.
    if (user == null) {
      logger.info("User {} doesn't exist in this manager cluster", email);
    } else {
      if (user.isTwoFactorAuthenticationEnabled()) {
        user = generate2faJWTToken(user);
      }
      // With identity service manage bearer tokens, no need for manager to generate bearer tokens.
      //      else {
      //        user = authService.generateBearerTokenForUser(user);
      //      }
    }
    return user;
  }

  public User generate2faJWTToken(User user) {
    String jwtToken = userService.generateJWTToken(user.getEmail(), JWT_CATEGORY.MULTIFACTOR_AUTH);
    return User.Builder.anUser()
        .withUuid(user.getUuid())
        .withEmail(user.getEmail())
        .withName(user.getName())
        .withTwoFactorAuthenticationMechanism(user.getTwoFactorAuthenticationMechanism())
        .withTwoFactorAuthenticationEnabled(user.isTwoFactorAuthenticationEnabled())
        .withTwoFactorJwtToken(jwtToken)
        .withAccounts(user.getAccounts())
        .withSupportAccounts(user.getSupportAccounts())
        .withDefaultAccountId(user.getDefaultAccountId())
        .build();
  }

  private String[] decryptBasicToken(String basicToken) {
    String[] decryptedData = decodeBase64ToString(basicToken).split(":");
    if (decryptedData.length < 2) {
      throw new WingsException(INVALID_CREDENTIAL, USER);
    }
    return decryptedData;
  }

  public User defaultLoginAccount(String basicToken, String accountId) {
    try {
      String[] decryptedData = decryptBasicToken(basicToken);
      String userName = decryptedData[0];
      String password = decryptedData[1];

      if (isNotEmpty(accountId)) {
        User user = authenticationUtils.getUser(userName, USER);
        AuthenticationMechanism authenticationMechanism = getAuthenticationMechanism(user, accountId);
        return defaultLoginInternal(userName, password, false, authenticationMechanism);
      } else {
        return defaultLogin(userName, password);
      }
    } catch (Exception e) {
      logger.warn("Failed to login via default mechanism", e);
      throw new WingsException(INVALID_CREDENTIAL, USER);
    }
  }

  public User defaultLogin(String basicToken) {
    return defaultLoginAccount(basicToken, null);
  }

  public User defaultLogin(String userName, String password) {
    return defaultLoginInternal(userName, password, false, getAuthenticationMechanism(userName));
  }

  public User defaultLoginUsingPasswordHash(String userName, String passwordHash) {
    return defaultLoginInternal(userName, passwordHash, true, getAuthenticationMechanism(userName));
  }

  private User defaultLoginInternal(
      String userName, String password, boolean isPasswordHash, AuthenticationMechanism authenticationMechanism) {
    try {
      AuthHandler authHandler = getAuthHandler(authenticationMechanism);
      if (authHandler == null) {
        logger.error("No auth handler found for auth mechanism {}", authenticationMechanism);
        throw new WingsException(INVALID_CREDENTIAL);
      }

      User user;
      if (isPasswordHash) {
        if (authHandler instanceof PasswordBasedAuthHandler) {
          PasswordBasedAuthHandler passwordBasedAuthHandler = (PasswordBasedAuthHandler) authHandler;
          user = passwordBasedAuthHandler.authenticateWithPasswordHash(userName, password).getUser();
        } else {
          logger.error("isPasswordHash should not be true if the auth mechanism {} is not username / password",
              authenticationMechanism);
          throw new WingsException(INVALID_CREDENTIAL);
        }
      } else {
        user = authHandler.authenticate(userName, password).getUser();
      }

      if (user.isTwoFactorAuthenticationEnabled()) {
        return generate2faJWTToken(user);
      } else {
        return authService.generateBearerTokenForUser(user);
      }
    } catch (Exception e) {
      logger.warn("Failed to login via default mechanism", e);
      throw new WingsException(INVALID_CREDENTIAL, USER);
    }
  }

  public User loginUsingHarnessPassword(final String basicToken) {
    String[] decryptedData = decryptBasicToken(basicToken);
    User user = defaultLoginInternal(decryptedData[0], decryptedData[1], false, AuthenticationMechanism.USER_PASSWORD);
    if (user == null) {
      throw new WingsException(USER_DOES_NOT_EXIST);
    }

    if (user.isDisabled()) {
      throw new WingsException(USER_DISABLED, USER);
    }

    String accountId = authenticationUtils.getPrimaryAccount(user).getUuid();

    if (!userService.isUserAccountAdmin(authService.getUserPermissionInfo(accountId, user), accountId)) {
      throw new WingsException(USER_NOT_AUTHORIZED, USER);
    }
    return user;
  }

  public User ssoRedirectLogin(String jwtSecret) {
    try {
      User user = userService.verifyJWTToken(jwtSecret, JWT_CATEGORY.SSO_REDIRECT);
      if (user == null) {
        throw new WingsException(USER_DOES_NOT_EXIST);
      }
      if (user.isTwoFactorAuthenticationEnabled()) {
        return generate2faJWTToken(user);
      } else {
        return authService.generateBearerTokenForUser(user);
      }
    } catch (Exception e) {
      logger.warn("Failed to login via SSO", e);
      throw new WingsException(INVALID_CREDENTIAL, USER);
    }
  }

  public Response samlLogin(String... credentials) throws URISyntaxException {
    try {
      User user = samlBasedAuthHandler.authenticate(credentials).getUser();
      String jwtToken = userService.generateJWTToken(user.getEmail(), JWT_CATEGORY.SSO_REDIRECT);
      String encodedApiUrl = encodeBase64(configuration.getApiUrl());

      Map<String, String> params = new HashMap<>();
      params.put("token", jwtToken);
      params.put("apiurl", encodedApiUrl);
      URI redirectUrl = authenticationUtils.buildAbsoluteUrl("/saml.html", params);
      return Response.seeOther(redirectUrl).build();
    } catch (Exception e) {
      logger.warn("Failed to login via saml", e);
      URI redirectUrl = new URI(getBaseUrl() + "#/login?errorCode=invalidsso");
      return Response.seeOther(redirectUrl).build();
    }
  }

  public String extractToken(String authorizationHeader, String prefix) {
    if (authorizationHeader == null || !authorizationHeader.startsWith(prefix)) {
      throw new WingsException(INVALID_TOKEN, USER_ADMIN);
    }
    return authorizationHeader.substring(prefix.length()).trim();
  }

  public String refreshToken(String oldToken) {
    String token = oldToken.substring("Bearer".length()).trim();
    return authService.refreshToken(token).getToken();
  }

  public Response oauth2CallbackUrl(String... credentials) throws URISyntaxException {
    try {
      if (!featureFlagService.isEnabled(FeatureName.OAUTH_LOGIN, null)) {
        throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED);
      }
      User user;
      AuthenticationResponse authenticationResponse = oauthBasedAuthHandler.authenticate(credentials);

      if (null == authenticationResponse.getUser()) {
        OauthAuthenticationResponse oauthAuthenticationResponse = (OauthAuthenticationResponse) authenticationResponse;
        user = userService.signUpUserUsingOauth(
            oauthAuthenticationResponse.getOauthUserInfo(), oauthAuthenticationResponse.getOauthClient());
      } else {
        user = authenticationResponse.getUser();
      }

      logger.info("OauthAuthentication succeeded for email {}", user.getEmail());
      String jwtToken = userService.generateJWTToken(user.getEmail(), JWT_CATEGORY.SSO_REDIRECT);
      String encodedApiUrl = encodeBase64(configuration.getApiUrl());

      Map<String, String> params = new HashMap<>();
      params.put("token", jwtToken);
      params.put("apiurl", encodedApiUrl);
      URI redirectUrl = authenticationUtils.buildAbsoluteUrl("/saml.html", params);

      return Response.seeOther(redirectUrl).build();
    } catch (Exception e) {
      logger.warn("Failed to login via oauth", e);
      URI redirectUrl = new URI(getBaseUrl() + "#/login?errorCode=invalidsso");
      return Response.seeOther(redirectUrl).build();
    }
  }

  public Response oauth2Redirect(final String provider) {
    OauthProviderType oauthProvider = OauthProviderType.valueOf(provider.toUpperCase());
    oauthOptions.getRedirectURI(oauthProvider);
    String returnURI = oauthOptions.getRedirectURI(oauthProvider);
    try {
      return Response.seeOther(new URI(returnURI)).build();
    } catch (URISyntaxException e) {
      throw new InvalidRequestException("Unable to generate the redirection URL", e);
    }
  }

  public String getBaseUrl() {
    String baseUrl = configuration.getPortal().getUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return baseUrl;
  }
}
