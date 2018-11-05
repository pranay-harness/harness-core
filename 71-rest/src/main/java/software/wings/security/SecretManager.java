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
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.app.MainConfiguration;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;

@Singleton
public class SecretManager {
  @Inject private MainConfiguration configuration;

  public enum JWT_CATEGORY {
    MULTIFACTOR_AUTH(3 * 60 * 1000), // 3 mins
    SSO_REDIRECT(60 * 1000), // 1 min
    PASSWORD_SECRET(4 * 60 * 60 * 1000), // 4 hrs
    ZENDESK_SECRET(4 * 60 * 60 * 1000), // 4 hrs
    EXTERNAL_SERVICE_SECRET(60 * 60 * 1000), // 1hr
    AUTH_SECRET(24 * 60 * 60 * 1000); // 24 hr

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
      default:
        return configuration.getPortal().getJwtMultiAuthSecret();
    }
  }

  public Map<String, Claim> verifyJWTToken(String jwtToken, JWT_CATEGORY category) {
    String jwtPasswordSecret = getJWTSecret(category);
    if (jwtPasswordSecret == null) {
      throw new InvalidRequestException("Could not find secret for " + category);
    }
    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer("Harness Inc").build();
      verifier.verify(jwtToken);
      return JWT.decode(jwtToken).getClaims();
    } catch (UnsupportedEncodingException | JWTDecodeException | SignatureVerificationException e) {
      throw new WingsException(INVALID_CREDENTIAL, USER)
          .addParam("message", "Invalid JWTToken received, failed to decode the token");
    } catch (InvalidClaimException invalidClaimException) {
      throw new WingsException(EXPIRED_TOKEN, USER).addParam("message", "Token expired");
    }
  }

  public String generateJWTToken(Map<String, String> claims, JWT_CATEGORY category) {
    String jwtPasswordSecret = getJWTSecret(category);
    if (jwtPasswordSecret == null) {
      throw new InvalidRequestException("Could not find secret for " + category);
    }

    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
      Builder jwtBuilder = JWT.create()
                               .withIssuer("Harness Inc")
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
}
