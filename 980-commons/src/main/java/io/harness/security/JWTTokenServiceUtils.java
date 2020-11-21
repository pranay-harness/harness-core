package io.harness.security;

import static io.harness.AuthorizationServiceHeader.DEFAULT;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.exception.WingsException.USER_SRE;

import static org.apache.commons.lang3.StringUtils.SPACE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.Claim;
import java.io.UnsupportedEncodingException;
import java.security.interfaces.RSAKey;
import java.util.Map;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@UtilityClass
@Slf4j
public class JWTTokenServiceUtils {
  private final String ISSUER = "Harness Inc";

  public boolean isServiceAuthorizationValid(String serviceToken, String serviceSecret) {
    verifyJWTToken(serviceToken, serviceSecret);
    return true;
  }

  public Map<String, Claim> verifyJWTToken(String jwtToken, String secret) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(secret);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
      verifier.verify(jwtToken);
      return JWT.decode(jwtToken).getClaims();
    } catch (UnsupportedEncodingException | JWTDecodeException | SignatureVerificationException e) {
      throw new InvalidRequestException(
          "Invalid JWTToken received, failed to decode the token", e, INVALID_TOKEN, USER);
    } catch (InvalidClaimException e) {
      throw new InvalidRequestException("Token expired", e, EXPIRED_TOKEN, USER);
    }
  }

  public Map<String, Claim> verifyJWTToken(String jwtToken, RSAKey secret) {
    try {
      Algorithm algorithm = Algorithm.RSA256(secret);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
      verifier.verify(jwtToken);
      return JWT.decode(jwtToken).getClaims();
    } catch (JWTDecodeException | SignatureVerificationException e) {
      throw new InvalidRequestException(
          "Invalid JWTToken received, failed to decode the token", e, INVALID_TOKEN, USER);
    } catch (InvalidClaimException e) {
      throw new InvalidRequestException("Token expired", e, EXPIRED_TOKEN, USER);
    }
  }

  public String extractToken(ContainerRequestContext requestContext, String prefix) {
    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader == null || !authorizationHeader.startsWith(prefix)) {
      throw new InvalidRequestException("Invalid token", INVALID_TOKEN, USER_ADMIN);
    }
    return authorizationHeader.substring(prefix.length()).trim();
  }

  public String extractSecret(Map<String, String> serviceToSecretMapping, String source) {
    if (serviceToSecretMapping != null && serviceToSecretMapping.containsKey(source)) {
      return serviceToSecretMapping.get(source);
    }
    if (serviceToSecretMapping != null && serviceToSecretMapping.containsKey(DEFAULT.getServiceId())) {
      return serviceToSecretMapping.get(DEFAULT.getServiceId());
    }
    throw new InvalidRequestException(String.format("Unknown Source [%s]", source), USER);
  }

  public String extractSource(ContainerRequestContext requestContext) {
    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader == null) {
      throw new InvalidRequestException("Invalid token", INVALID_TOKEN, USER_SRE);
    }

    String[] sourceAndToken = authorizationHeader.trim().split(SPACE, 2);
    if (sourceAndToken.length < 2) {
      throw new InvalidRequestException("Invalid token", INVALID_TOKEN, USER_SRE);
    }
    return sourceAndToken[0].trim();
  }
}
