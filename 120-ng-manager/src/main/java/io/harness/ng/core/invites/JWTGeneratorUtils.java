package io.harness.ng.core.invites;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.security.JWTTokenServiceUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.Claim;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//  I want to have JWTGeneratorUtils as util class but how will I inject dependencies then?
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(PL)
public class JWTGeneratorUtils {
  public static final String HARNESS_USER = "harnessUser";
  // A claim in the token to denote which environment the token is generated.
  public static final String ENV = "env";

  private static final String ISSUER = "Harness Inc";

  public String generateJWTToken(Map<String, String> claims, Long validityDurationInMillis, String jwtPasswordSecret) {
    if (jwtPasswordSecret == null) {
      throw new InvalidRequestException("Could not find verification secret token");
    }
    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
      JWTCreator.Builder jwtBuilder = JWT.create().withIssuer(ISSUER).withIssuedAt(new Date());

      if (validityDurationInMillis != null) {
        jwtBuilder.withExpiresAt(new Date(System.currentTimeMillis() + validityDurationInMillis));
      }
      if (!isEmpty(claims)) {
        claims.forEach(jwtBuilder::withClaim);
      }
      return jwtBuilder.sign(algorithm);
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new JWTCreationException("JWTToken could not be generated", exception);
    }
  }

  public Map<String, Claim> verifyJWTToken(String token, String jwtPasswordSecret) {
    return JWTTokenServiceUtils.verifyJWTToken(token, jwtPasswordSecret);
  }
}
