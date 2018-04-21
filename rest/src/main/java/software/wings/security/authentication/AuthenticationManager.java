package software.wings.security.authentication;

import static software.wings.beans.ErrorCode.INVALID_TOKEN;
import static software.wings.beans.ErrorCode.USER_DOES_NOT_EXIST;
import static software.wings.exception.WingsException.USER;
import static software.wings.exception.WingsException.USER_ADMIN;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.authentication.LoginTypeResponse.LoginTypeResponseBuilder;
import software.wings.security.saml.SamlClientService;
import software.wings.security.saml.SamlRequest;
import software.wings.service.intfc.UserService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;

@Singleton
public class AuthenticationManager {
  @Inject private PasswordBasedAuthHandler passwordBasedAuthHandler;
  @Inject private SamlBasedAuthHandler samlBasedAuthHandler;
  @Inject private AuthenticationUtil authenticationUtil;
  @Inject private SamlClientService samlClientService;
  @Inject private MainConfiguration configuration;
  @Inject private UserService userService;
  @Inject private WingsPersistence wingsPersistence;

  private static Logger logger = LoggerFactory.getLogger(AuthenticationManager.class);

  public AuthHandler getAuthHandler(AuthenticationMechanism mechanism) {
    switch (mechanism) {
      case SAML:
        return samlBasedAuthHandler;
      default:
        return passwordBasedAuthHandler;
    }
  }

  private AuthenticationMechanism getAuthenticationMechanism(User user) {
    AuthenticationMechanism authenticationMechanism = AuthenticationMechanism.USER_PASSWORD;
    /**
     * If the number of accounts > 1, by default assume it to be USER_PASSWORD.
     * Typically this should only be for Harness users.
     * All other customers should have only 1 account mapped to their users
     */
    if (user.getAccounts().size() == 1) {
      Account account = user.getAccounts().get(0);
      authenticationMechanism = account.getAuthenticationMechanism();
      if (authenticationMechanism == null) {
        authenticationMechanism = AuthenticationMechanism.USER_PASSWORD;
      }
    }
    return authenticationMechanism;
  }

  public AuthenticationMechanism getAuthenticationMechanism(String userName) {
    return getAuthenticationMechanism(authenticationUtil.getUser(userName));
  }

  public LoginTypeResponse getLoginTypeResponse(String userName) {
    User user = authenticationUtil.getUser(userName, USER);

    AuthenticationMechanism authenticationMechanism = getAuthenticationMechanism(user);

    LoginTypeResponseBuilder builder = LoginTypeResponse.builder();
    switch (authenticationMechanism) {
      case SAML:
        SamlRequest samlRequest = samlClientService.generateSamlRequest(user);
        builder.samlRequest(samlRequest);
        break;
      default:
        // Nothing to do by default
    }

    return builder.authenticationMechanism(authenticationMechanism).build();
  }

  private User generate2faJWTToken(User user) {
    String jwtToken = userService.generateJWTToken(user.getEmail(), JWT_CATEGORY.MULTIFACTOR_AUTH);
    return User.Builder.anUser()
        .withEmail(user.getEmail())
        .withName(user.getName())
        .withTwoFactorAuthenticationMechanism(user.getTwoFactorAuthenticationMechanism())
        .withTwoFactorAuthenticationEnabled(user.isTwoFactorAuthenticationEnabled())
        .withTwoFactorJwtToken(jwtToken)
        .build();
  }
  public User defaultLogin(String... credentials) {
    User user = passwordBasedAuthHandler.authenticate(credentials);
    if (user.isTwoFactorAuthenticationEnabled()) {
      return generate2faJWTToken(user);
    } else {
      return authenticationUtil.generateBearerTokenForUser(user);
    }
  }

  public User ssoRedirectLogin(String jwtSecret) {
    User user = userService.verifyJWTToken(jwtSecret, JWT_CATEGORY.SSO_REDIRECT);
    if (user == null) {
      throw new WingsException(USER_DOES_NOT_EXIST);
    }
    if (user.isTwoFactorAuthenticationEnabled()) {
      return generate2faJWTToken(user);
    } else {
      return authenticationUtil.generateBearerTokenForUser(user);
    }
  }

  public Response samlLogin(String... credentials) throws URISyntaxException {
    try {
      User user = samlBasedAuthHandler.authenticate(credentials);
      String jwtToken = userService.generateJWTToken(user.getEmail(), JWT_CATEGORY.SSO_REDIRECT);
      String encodedApiUrl = new String(Base64.getEncoder().encode(configuration.getPortal().getUrl().getBytes()));
      Map<String, String> params = new HashMap<>();
      params.put("token", jwtToken);
      params.put("apiurl", encodedApiUrl);
      URI redirectUrl = authenticationUtil.buildAbsoluteUrl("/saml.html", params);
      return Response.seeOther(redirectUrl).build();
    } catch (Exception e) {
      logger.warn("Failed to login via saml", e);
      URI redirectUrl = new URI(authenticationUtil.getBaseUrl() + "#/login?errorCode=invalidsso");
      return Response.seeOther(redirectUrl).build();
    }
  }

  public String extractToken(String authorizationHeader, String prefix) {
    if (authorizationHeader == null || !authorizationHeader.startsWith(prefix)) {
      throw new WingsException(INVALID_TOKEN, USER_ADMIN);
    }
    return authorizationHeader.substring(prefix.length()).trim();
  }
}
