package io.harness.security;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;

import com.google.common.base.Preconditions;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by rsingh on 9/17/18.
 */
public class ServiceTokenGenerator {
  public static final AtomicReference<String> VERIFICATION_SERVICE_SECRET = new AtomicReference<>();

  public String getVerificationServiceToken() {
    Preconditions.checkState(isNotEmpty(VERIFICATION_SERVICE_SECRET.get()),
        "could not read verification secret from system or env properties");
    return createNewToken(VERIFICATION_SERVICE_SECRET.get());
  }

  public String getServiceToken(String secretKey) {
    if (isNotEmpty(secretKey)) {
      return createNewToken(secretKey);
    }
    throw new InvalidArgumentsException("secretKey cannot be empty", null);
  }

  private String createNewToken(String serviceSecret) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(serviceSecret);

      return JWT.create()
          .withIssuer("Harness Inc")
          .withIssuedAt(new Date())
          .withExpiresAt(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(4)))
          .withNotBefore(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)))
          .withIssuedAt(new Date())
          .sign(algorithm);
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new WingsException(GENERAL_ERROR).addParam("message", "reset password link could not be generated");
    }
  }
}
