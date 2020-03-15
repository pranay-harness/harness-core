package software.wings.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;

@Slf4j
@Singleton
public class SecretManager {
  // A boolean claim to denote if the user is in harness user group
  public static final String HARNESS_USER = "harnessUser";
  // A claim in the token to denote which environment the token is generated.
  public static final String ENV = "env";

  private static final String ISSUER = "Harness Inc";

  @Inject private MainConfiguration configuration;

  private static ObjectMapper objectMapper = new ObjectMapper();

  public enum JWT_CATEGORY {
    MULTIFACTOR_AUTH(3 * 60 * 1000), // 3 mins
    SSO_REDIRECT(60 * 1000), // 1 min
    OAUTH_REDIRECT(3 * 60 * 1000), // 1 min
    PASSWORD_SECRET(4 * 60 * 60 * 1000), // 4 hrs
    ZENDESK_SECRET(4 * 60 * 60 * 1000), // 4 hrs
    EXTERNAL_SERVICE_SECRET(60 * 60 * 1000), // 1hr
    IDENTITY_SERVICE_SECRET(60 * 60 * 1000), // 1hr
    AUTH_SECRET(24 * 60 * 60 * 1000), // 24 hr
    JIRA_SERVICE_SECRET(7 * 24 * 60 * 60 * 1000), // 7 days
    MARKETPLACE_SIGNUP(24 * 60 * 60 * 1000), // 1 day
    API_KEY(10 * 60 * 1000), // 10 mins; API_KEY secret is not configured in config.yml!
    DATA_HANDLER_SECRET(60 * 60 * 1000);
    private int validityDuration;

    JWT_CATEGORY(int validityDuration) {
      this.validityDuration = validityDuration;
    }

    public int getValidityDuration() {
      return validityDuration;
    }
  }

  public String getJWTSecret(JWT_CATEGORY category) {
    switch (category) {
      case MULTIFACTOR_AUTH:
        return configuration.getPortal().getJwtMultiAuthSecret();
      case ZENDESK_SECRET:
        return configuration.getPortal().getJwtZendeskSecret();
      case PASSWORD_SECRET:
        return configuration.getPortal().getJwtPasswordSecret();
      case EXTERNAL_SERVICE_SECRET:
        return configuration.getPortal().getJwtExternalServiceSecret();
      case SSO_REDIRECT:
        return configuration.getPortal().getJwtSsoRedirectSecret();
      case AUTH_SECRET:
        return configuration.getPortal().getJwtAuthSecret();
      case JIRA_SERVICE_SECRET:
        return configuration.getPortal().getJwtExternalServiceSecret();
      case DATA_HANDLER_SECRET:
        return configuration.getPortal().getJwtDataHandlerSecret();
      case MARKETPLACE_SIGNUP:
        return configuration.getPortal().getJwtMarketPlaceSecret();
      case IDENTITY_SERVICE_SECRET:
        return configuration.getPortal().getJwtIdentityServiceSecret();
      default:
        return configuration.getPortal().getJwtMultiAuthSecret();
    }
  }

  public Map<String, Claim> verifyJWTToken(String jwtToken, JWT_CATEGORY category) {
    String jwtPasswordSecret = getJWTSecret(category);
    if (jwtPasswordSecret == null) {
      throw new InvalidRequestException("Could not find secret for " + category);
    }
    return verifyJWTToken(jwtToken, jwtPasswordSecret, category);
  }

  public Map<String, Claim> verifyJWTToken(String jwtToken, String secret, JWT_CATEGORY category) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(secret);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
      verifier.verify(jwtToken);
      return JWT.decode(jwtToken).getClaims();
    } catch (UnsupportedEncodingException | JWTDecodeException | SignatureVerificationException e) {
      logger.info("Failed to verify JWT token {} in category {} with error: {}", jwtToken, category, e.getMessage());
      throw new WingsException(INVALID_CREDENTIAL, USER, e)
          .addParam("message", "Invalid JWTToken received, failed to decode the token");
    } catch (InvalidClaimException e) {
      logger.info("Failed to verify JWT token {} in category {} with error: {}", jwtToken, category, e.getMessage());
      throw new WingsException(EXPIRED_TOKEN, USER, e).addParam("message", "Token expired");
    }
  }

  HarnessUserAccountActions getHarnessUserAccountActions(Map<String, Claim> claims) {
    Claim claim = claims.get(HARNESS_USER);
    if (claim != null && !claim.isNull() && claim.asString().length() > 0) {
      try {
        return deserializeAccountActions(claim.asString());
      } catch (IOException e) {
        logger.error("Failed to deserialize the harness user account actions data object from token", e);
      }
    }
    return null;
  }

  public static String serializeAccountActions(HarnessUserAccountActions harnessUserAccountActions) throws IOException {
    return objectMapper.writeValueAsString(harnessUserAccountActions);
  }

  static HarnessUserAccountActions deserializeAccountActions(String serializedAccountActions) throws IOException {
    return objectMapper.readValue(serializedAccountActions, HarnessUserAccountActions.class);
  }

  public String generateJWTToken(Map<String, String> claims, JWT_CATEGORY category) {
    String jwtPasswordSecret = getJWTSecret(category);
    if (jwtPasswordSecret == null) {
      throw new InvalidRequestException("Could not find secret for " + category);
    }
    return generateJWTToken(claims, jwtPasswordSecret, category);
  }

  public String generateJWTToken(Map<String, String> claims, String secret, JWT_CATEGORY category) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(secret);
      Builder jwtBuilder = JWT.create()
                               .withIssuer(ISSUER)
                               .withIssuedAt(new Date())
                               .withExpiresAt(new Date(System.currentTimeMillis() + category.getValidityDuration()));
      if (!isEmpty(claims)) {
        claims.forEach(jwtBuilder::withClaim);
      }
      return jwtBuilder.sign(algorithm);
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new WingsException(GENERAL_ERROR, exception).addParam("message", "JWTToken could not be generated");
    }
  }

  public String generateJWTTokenWithCustomTimeOut(Map<String, String> claims, String secret, int tokenValidDuration) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(secret);
      Builder jwtBuilder = JWT.create()
                               .withIssuer(ISSUER)
                               .withIssuedAt(new Date())
                               .withExpiresAt(new Date(System.currentTimeMillis() + tokenValidDuration));
      if (!isEmpty(claims)) {
        claims.forEach(jwtBuilder::withClaim);
      }
      return jwtBuilder.sign(algorithm);
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new WingsException(GENERAL_ERROR, exception).addParam("message", "JWTToken could not be generated");
    }
  }
}