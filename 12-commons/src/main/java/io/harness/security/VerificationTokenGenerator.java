package io.harness.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;

import com.google.common.base.Preconditions;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import io.harness.exception.WingsException;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 9/17/18.
 */
public class VerificationTokenGenerator {
  public static final String VERIFICATION_SERVICE_SECRET = "verification_service_secret";

  public String getToken() {
    String verificationSecret = System.getenv(VERIFICATION_SERVICE_SECRET);
    if (isEmpty(verificationSecret)) {
      verificationSecret = System.getProperty(VERIFICATION_SERVICE_SECRET);
    }

    Preconditions.checkState(
        isNotEmpty(verificationSecret), "could not read verification secret from system or env properties");
    try {
      Algorithm algorithm = Algorithm.HMAC256(verificationSecret);

      return JWT.create()
          .withIssuer("Harness Inc")
          .withIssuedAt(new Date())
          .withExpiresAt(new Date(System.currentTimeMillis() + 4 * 60 * 60 * 1000)) // 4 hrs
          .withNotBefore(new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)))
          .withIssuedAt(new Date())
          .sign(algorithm);
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new WingsException(GENERAL_ERROR).addParam("message", "reset password link could not be generated");
    }
  }
}
