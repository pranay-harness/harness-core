package software.wings.security.authentication;

import static io.harness.data.encoding.EncodingUtils.decodeBase64ToString;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.DOMAIN_WHITELIST_FILTER_CHECK_FAILED;
import static io.harness.eraro.ErrorCode.EMAIL_NOT_VERIFIED;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.eraro.ErrorCode.MAX_FAILED_ATTEMPT_COUNT_EXCEEDED;
import static io.harness.eraro.ErrorCode.PASSWORD_EXPIRED;
import static io.harness.eraro.ErrorCode.USER_DISABLED;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static io.harness.eraro.ErrorCode.USER_LOCKED;
import static io.harness.eraro.ErrorCode.USER_NOT_AUTHORIZED;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static org.apache.cxf.common.util.UrlUtils.urlDecode;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.configuration.DeployMode;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidCredentialsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.authentication.LoginTypeResponse.LoginTypeResponseBuilder;
import software.wings.security.authentication.oauth.OauthBasedAuthHandler;
import software.wings.security.authentication.oauth.OauthOptions;
import software.wings.security.authentication.recaptcha.FailedLoginAttemptCountChecker;
import software.wings.security.authentication.recaptcha.MaxLoginAttemptExceededException;
import software.wings.security.saml.SSORequest;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
  @Inject private OauthBasedAuthHandler oauthBasedAuthHandler;
  @Inject private OauthOptions oauthOptions;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private FailedLoginAttemptCountChecker failedLoginAttemptCountChecker;

  private static final String LOGIN_ERROR_CODE_INVALIDSSO = "#/login?errorCode=invalidsso";
  private static final String LOGIN_ERROR_CODE_SAMLTESTSUCCESS = "#/login?errorCode=samltestsuccess";
  private static final String EMAIL = "email";
  private static final String ACCOUNT_ID = "accountId";

  private static final List<ErrorCode> NON_INVALID_CREDENTIALS_ERROR_CODES =
      Arrays.asList(USER_LOCKED, PASSWORD_EXPIRED, MAX_FAILED_ATTEMPT_COUNT_EXCEEDED);

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
      Preconditions.checkNotNull(user.getAccounts(), String.format("No account found for {}", user.getEmail()));
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

  LoginTypeResponse getLoginTypeResponse(String userName) {
    return getLoginTypeResponse(userName, null);
  }

  public LoginTypeResponse getLoginTypeResponseForOnPrem() {
    if (mainConfiguration.getDeployMode() != null && !DeployMode.isOnPrem(mainConfiguration.getDeployMode().name())) {
      throw new InvalidRequestException("This API should only be called for on-prem deployments.");
    }
    List<Account> accounts = accountService.listAllAccounts();
    if (accounts.size() > 1) {
      logger.warn(
          "On-prem deployments are expected to have exactly 1 account. Returning response for the primary account");
    }
    // It is assumed that an on-prem deployment has exactly 1 account
    // as discussed with Vikas and Jesse
    Account account = accountService.getOnPremAccount().orElseThrow(
        () -> new InvalidRequestException("No Account found in the database"));
    User user = userService.getUsersOfAccount(account.getUuid()).get(0);
    return getLoginTypeResponse(urlDecode(user.getEmail()), account.getUuid());
  }

  public LoginTypeResponse getLoginTypeResponse(String userName, String accountId) {
    final LoginTypeResponseBuilder builder = LoginTypeResponse.builder();

    /*
     * To prevent possibility of user enumeration (https://harness.atlassian.net/browse/HAR-7188),
     * instead of throwing the USER_DOES_NOT_EXIST exception, send USER_PASSWORD as the login mechanism.
     * The next page throws INVALID_CREDENTIAL exception in case of wrong userId/password which doesn't reveals any
     * information.
     */
    User user = null;
    try {
      user = authenticationUtils.getUser(userName, USER);
    } catch (WingsException ex) {
      if (ex.getCode() == ErrorCode.USER_DOES_NOT_EXIST && mainConfiguration.getDeployMode() != null
          && DeployMode.isOnPrem(mainConfiguration.getDeployMode().name())) {
        return builder.authenticationMechanism(AuthenticationMechanism.USER_PASSWORD).build();
      }
      throw ex;
    }

    boolean showCaptcha = false;
    try {
      failedLoginAttemptCountChecker.check(user);
    } catch (MaxLoginAttemptExceededException e) {
      logger.info("User exceeded max failed login attemts. {}", e.getMessage());
      showCaptcha = true;
    }

    builder.showCaptcha(showCaptcha);
    Account account = userService.getAccountByIdIfExistsElseGetDefaultAccount(
        user, isEmpty(accountId) ? Optional.empty() : Optional.of(accountId));
    AuthenticationMechanism authenticationMechanism = account.getAuthenticationMechanism();
    if (null == authenticationMechanism) {
      authenticationMechanism = AuthenticationMechanism.USER_PASSWORD;
    }
    builder.isOauthEnabled(account.isOauthEnabled());
    if (account.isOauthEnabled()) {
      builder.SSORequest(oauthOptions.createOauthSSORequest(account.getUuid()));
    }

    SSORequest ssoRequest;
    switch (authenticationMechanism) {
      case USER_PASSWORD:
        if (!user.isEmailVerified() && !DeployMode.isOnPrem(mainConfiguration.getDeployMode().getDeployedAs())) {
          // HAR-7984: Return 401 http code if user email not verified yet.
          throw new WingsException(EMAIL_NOT_VERIFIED, USER);
        }
        break;
      case SAML:
        ssoRequest = samlClientService.generateSamlRequest(user);
        builder.SSORequest(ssoRequest);
        break;
      case OAUTH:
      case LDAP: // No need to build anything extra for the response.
      default:
        // Nothing to do by default
    }
    return builder.authenticationMechanism(authenticationMechanism).build();
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
      //    PL-3163: LDAP/SAML users are not email-verified, but we need to allow them to login.
      //    } else if (!user.isEmailVerified()) {
      //      logger.info("User {} is not yet email verified in this manager cluster, login is not allowed.", email);
      //      throw new WingsException(EMAIL_NOT_VERIFIED, USER);
    } else if (user.isDisabled()) {
      logger.info("User {} is disabled in this manager cluster, login is not allowed.", email);
      throw new WingsException(USER_DISABLED, USER);
    } else {
      if (user.isTwoFactorAuthenticationEnabled()) {
        user = generate2faJWTToken(user);
      } else {
        // PL-2698: UI lead-update call will be called only if it's first login. Will need to
        // make sure the firstLogin is derived from lastLogin value.
        boolean isFirstLogin = user.getLastLogin() == 0L;
        user.setFirstLogin(isFirstLogin);

        // User's lastLogin field should be updated on every login attempt.
        user.setLastLogin(System.currentTimeMillis());
        userService.update(user);
      }
    }

    return user;
  }

  public User generate2faJWTToken(User user) {
    HashMap<String, String> claimMap = new HashMap<>();
    claimMap.put(EMAIL, user.getEmail());
    String jwtToken = userService.generateJWTToken(claimMap, JWT_CATEGORY.MULTIFACTOR_AUTH);
    return User.Builder.anUser()
        .uuid(user.getUuid())
        .email(user.getEmail())
        .name(user.getName())
        .twoFactorAuthenticationMechanism(user.getTwoFactorAuthenticationMechanism())
        .twoFactorAuthenticationEnabled(user.isTwoFactorAuthenticationEnabled())
        .twoFactorJwtToken(jwtToken)
        .accounts(user.getAccounts())
        .supportAccounts(user.getSupportAccounts())
        .defaultAccountId(user.getDefaultAccountId())
        .emailVerified(user.isEmailVerified())
        .build();
  }

  private String[] decryptBasicToken(String basicToken) {
    String[] decryptedData = decodeBase64ToString(basicToken).split(":", 2);
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
    } catch (WingsException e) {
      if (e.getCode() == ErrorCode.DOMAIN_WHITELIST_FILTER_CHECK_FAILED) {
        throw new WingsException(DOMAIN_WHITELIST_FILTER_CHECK_FAILED, USER);
      } else if (e.getCode() == ErrorCode.USER_DOES_NOT_EXIST) {
        throw new InvalidCredentialsException(INVALID_CREDENTIAL.name(), USER, e);
      }
      throw e;
    } catch (Exception e) {
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
    } catch (WingsException we) {
      logger.warn("Failed to login via default mechanism", we);
      if (NON_INVALID_CREDENTIALS_ERROR_CODES.contains(we.getCode())) {
        throw we;
      } else {
        throw new WingsException(INVALID_CREDENTIAL, USER);
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

    String accountId = authenticationUtils.getDefaultAccount(user).getUuid();

    if (!userService.isUserAccountAdmin(authService.getUserPermissionInfo(accountId, user, false), accountId)) {
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
      HashMap<String, String> claimMap = new HashMap<>();
      claimMap.put(EMAIL, user.getEmail());
      claimMap.put("subDomainUrl", accountService.get(user.getDefaultAccountId()).getSubdomainUrl());
      claimMap.put(ACCOUNT_ID, user.getDefaultAccountId());

      String jwtToken = userService.generateJWTToken(claimMap, JWT_CATEGORY.SSO_REDIRECT);
      String encodedApiUrl = encodeBase64(configuration.getApiUrl());

      Map<String, String> params = getRedirectParamsForSsoRedirection(jwtToken, encodedApiUrl);
      URI redirectUrl = authenticationUtils.buildAbsoluteUrl("/saml.html", params, user.getDefaultAccountId());
      return Response.seeOther(redirectUrl).build();
    } catch (WingsException e) {
      if (e.getCode() == ErrorCode.SAML_TEST_SUCCESS_MECHANISM_NOT_ENABLED) {
        URI redirectUrl = new URI(getBaseUrl() + LOGIN_ERROR_CODE_SAMLTESTSUCCESS);
        return Response.seeOther(redirectUrl).build();
      } else {
        return generateInvalidSSOResponse(e);
      }
    } catch (Exception e) {
      return generateInvalidSSOResponse(e);
    }
  }

  private Map<String, String> getRedirectParamsForSsoRedirection(String jwtToken, String encodedApiUrl) {
    Boolean isOnprem =
        mainConfiguration.getDeployMode() != null && DeployMode.isOnPrem(mainConfiguration.getDeployMode().name());

    Map<String, String> params = new HashMap<>();
    params.put("token", jwtToken);
    params.put("apiurl", encodedApiUrl);
    params.put("onprem", isOnprem.toString());
    return params;
  }

  private Response generateInvalidSSOResponse(Exception e) throws URISyntaxException {
    logger.warn("Failed to login via saml", e);
    URI redirectUrl = new URI(getBaseUrl() + LOGIN_ERROR_CODE_INVALIDSSO);
    return Response.seeOther(redirectUrl).build();
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
      User user;
      AuthenticationResponse authenticationResponse = oauthBasedAuthHandler.authenticate(credentials);

      if (null == authenticationResponse.getUser()) {
        OauthAuthenticationResponse oauthAuthenticationResponse = (OauthAuthenticationResponse) authenticationResponse;
        user = userService.signUpUserUsingOauth(
            oauthAuthenticationResponse.getOauthUserInfo(), oauthAuthenticationResponse.getOauthClient().getName());
      } else {
        user = authenticationResponse.getUser();
      }

      logger.info("OauthAuthentication succeeded for email {}", user.getEmail());
      HashMap<String, String> claimMap = new HashMap<>();
      claimMap.put(EMAIL, user.getEmail());
      String jwtToken = userService.generateJWTToken(claimMap, JWT_CATEGORY.SSO_REDIRECT);
      String encodedApiUrl = encodeBase64(configuration.getApiUrl());

      Map<String, String> params = getRedirectParamsForSsoRedirection(jwtToken, encodedApiUrl);
      URI redirectUrl = authenticationUtils.buildAbsoluteUrl("/saml.html", params, user.getDefaultAccountId());

      return Response.seeOther(redirectUrl).build();
    } catch (Exception e) {
      logger.warn("Failed to login via oauth", e);
      URI redirectUrl = new URI(getBaseUrl() + LOGIN_ERROR_CODE_INVALIDSSO);
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
