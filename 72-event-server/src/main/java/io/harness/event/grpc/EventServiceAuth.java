package io.harness.event.grpc;

import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.exception.WingsException.USER_ADMIN;
import static software.wings.beans.ServiceSecretKey.ServiceType.EVENT_SERVICE;

import com.google.inject.Inject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import io.harness.exception.WingsException;
import io.harness.grpc.auth.AuthService;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.ServiceSecretKey;
import software.wings.beans.ServiceSecretKey.ServiceSecretKeyKeys;

import java.util.concurrent.TimeUnit;

@Slf4j
public class EventServiceAuth implements AuthService {
  private final HPersistence hPersistence;

  @Inject
  public EventServiceAuth(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  @Override
  public void validateToken(String token) {
    String secretKey = getServiceSecretKey();
    if (StringUtils.isBlank(secretKey)) {
      throw new WingsException("no secret key for service found for " + EVENT_SERVICE);
    }
    try {
      Algorithm algorithm = Algorithm.HMAC256(secretKey);
      JWTVerifier verifier =
          JWT.require(algorithm).withIssuer("Harness Inc").acceptIssuedAt(TimeUnit.MINUTES.toSeconds(10)).build();
      verifier.verify(token);
      JWT decode = JWT.decode(token);
      if (decode.getExpiresAt().getTime() < System.currentTimeMillis()) {
        throw new WingsException(EXPIRED_TOKEN, USER_ADMIN);
      }
    } catch (Exception ex) {
      logger.warn("Error in verifying JWT token ", ex);
      throw ex instanceof JWTVerificationException ? new WingsException(INVALID_TOKEN) : new WingsException(ex);
    }
  }

  private String getServiceSecretKey() {
    return hPersistence.createQuery(ServiceSecretKey.class)
        .filter(ServiceSecretKeyKeys.serviceType, EVENT_SERVICE)
        .get()
        .getServiceSecret();
  }
}
