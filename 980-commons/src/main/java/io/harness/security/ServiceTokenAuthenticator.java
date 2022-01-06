/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.exception.WingsException.USER_ADMIN;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Builder
@Slf4j
public class ServiceTokenAuthenticator {
  public static final long ALLOWED_SERVICE_TOKEN_ISSUE_LEEWAY_IN_SEC = TimeUnit.MINUTES.toSeconds(60);
  public static final String ISSUER = "Harness Inc";

  private final String secretKey;

  public void authenticate(String serviceToken) {
    try {
      validateJwtToken(serviceToken);
    } catch (JWTVerificationException jwtVerificationException) {
      throw new InvalidRequestException("invalid token", jwtVerificationException, INVALID_TOKEN, USER_ADMIN);
    } catch (WingsException we) {
      throw we;
    } catch (Exception ex) {
      throw new InvalidRequestException("Error while validating user token", ex);
    }
  }

  private void validateJwtToken(String serviceToken) throws UnsupportedEncodingException {
    ensureValidTokenStructure(serviceToken);
    JWT decode = JWT.decode(serviceToken);
    ensureActiveToken(decode);
  }

  private void ensureValidTokenStructure(String serviceToken) throws UnsupportedEncodingException {
    createJWTVerifier().verify(serviceToken);
  }

  private void ensureActiveToken(JWT decode) {
    if (decode.getExpiresAt().getTime() < System.currentTimeMillis()) {
      throw new InvalidRequestException("token has expired", EXPIRED_TOKEN, USER_ADMIN);
    }
  }

  private Algorithm getAlgorithm() throws UnsupportedEncodingException {
    return Algorithm.HMAC256(secretKey);
  }

  private JWTVerifier createJWTVerifier() throws UnsupportedEncodingException {
    return JWT.require(getAlgorithm())
        .withIssuer(ISSUER)
        .acceptIssuedAt(ALLOWED_SERVICE_TOKEN_ISSUE_LEEWAY_IN_SEC)
        .build();
  }
}
