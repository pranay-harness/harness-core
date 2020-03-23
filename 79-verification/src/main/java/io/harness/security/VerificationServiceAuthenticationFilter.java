package io.harness.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.ACCESS_DENIED;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jwt.EncryptedJWT;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.utils.AccountCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Account;
import software.wings.beans.HarnessApiKey.ClientType;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.dl.WingsPersistence;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.HarnessApiKeyAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.VerificationService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Priority;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

@Singleton
@Priority(AUTHENTICATION)
@Slf4j
public class VerificationServiceAuthenticationFilter implements ContainerRequestFilter {
  @Context private ResourceInfo resourceInfo;
  @Inject private AccountCache accountCache;
  @Inject private VerificationService learningEngineService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private VerificationManagerClient verificationManagerClient;

  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {
    if (authenticationExemptedRequests(containerRequestContext)) {
      return;
    }

    if (isHarnessClientApi(resourceInfo)) {
      String apiKeyFromHeader = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
      ClientType[] clientTypes = getClientTypesFromHarnessApiKeyAuth(resourceInfo);
      if (isEmpty(clientTypes)) {
        throw new WingsException(INVALID_TOKEN, USER);
      }
      verificationManagerClient.validateHarnessApiKey(clientTypes[0].name(), apiKeyFromHeader);
      return;
    }

    String authorization = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorization == null) {
      throw new WingsException(INVALID_TOKEN, USER);
    }

    if (isDelegateRequest(containerRequestContext)) {
      validateDelegateRequest(containerRequestContext);
      return;
    }

    if (isLearningEngineServiceRequest(containerRequestContext)) {
      validateLearningEngineServiceToken(
          substringAfter(containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "LearningEngine "));
      return;
    }

    throw new WingsException(INVALID_CREDENTIAL, USER);
  }

  private ClientType[] getClientTypesFromHarnessApiKeyAuth(ResourceInfo resourceInfo) {
    Method resourceMethod = resourceInfo.getResourceMethod();
    HarnessApiKeyAuth methodAnnotations = resourceMethod.getAnnotation(HarnessApiKeyAuth.class);
    if (null != methodAnnotations) {
      return methodAnnotations.clientTypes();
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    HarnessApiKeyAuth classAnnotations = resourceClass.getAnnotation(HarnessApiKeyAuth.class);
    if (null != classAnnotations) {
      return classAnnotations.clientTypes();
    }

    return null;
  }

  private boolean isHarnessClientApi(ResourceInfo resourceInfo) {
    return resourceInfo.getResourceMethod().getAnnotation(HarnessApiKeyAuth.class) != null
        || resourceInfo.getResourceClass().getAnnotation(HarnessApiKeyAuth.class) != null;
  }

  protected boolean authenticationExemptedRequests(ContainerRequestContext requestContext) {
    return requestContext.getMethod().equals(OPTIONS) || publicAPI()
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/version")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/swagger")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/swagger.json");
  }

  private boolean isDelegateRequest(ContainerRequestContext requestContext) {
    return delegateAPI() && startsWith(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "Delegate ");
  }

  private boolean delegateAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(DelegateAuth.class) != null
        || resourceClass.getAnnotation(DelegateAuth.class) != null;
  }

  protected void validateDelegateRequest(ContainerRequestContext containerRequestContext) {
    MultivaluedMap<String, String> pathParameters = containerRequestContext.getUriInfo().getPathParameters();
    MultivaluedMap<String, String> queryParameters = containerRequestContext.getUriInfo().getQueryParameters();

    String accountId = getRequestParamFromContext("accountId", pathParameters, queryParameters);
    String header = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (header.contains("Delegate")) {
      validateDelegateToken(
          accountId, substringAfter(containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "Delegate "));
    } else {
      throw new IllegalStateException("Invalid header:" + header);
    }
  }

  private String getRequestParamFromContext(
      String key, MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    return queryParameters.getFirst(key) != null ? queryParameters.getFirst(key) : pathParameters.getFirst(key);
  }

  protected boolean publicAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(PublicApi.class) != null
        || resourceClass.getAnnotation(PublicApi.class) != null;
  }

  private boolean isLearningEngineServiceRequest(ContainerRequestContext requestContext) {
    return learningEngineServiceAPI()
        && startsWith(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "LearningEngine ");
  }

  protected boolean learningEngineServiceAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(LearningEngineAuth.class) != null
        || resourceClass.getAnnotation(LearningEngineAuth.class) != null;
  }

  private void validateLearningEngineServiceToken(String learningEngineServiceToken) {
    String jwtLearningEngineServiceSecret = learningEngineService.getVerificationServiceSecretKey();
    if (StringUtils.isBlank(jwtLearningEngineServiceSecret)) {
      throw new InvalidRequestException("no secret key for service found for " + ServiceType.LEARNING_ENGINE);
    }
    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtLearningEngineServiceSecret);
      JWTVerifier verifier =
          JWT.require(algorithm).withIssuer("Harness Inc").acceptIssuedAt(TimeUnit.MINUTES.toSeconds(60)).build();
      verifier.verify(learningEngineServiceToken);
      JWT decode = JWT.decode(learningEngineServiceToken);
      if (decode.getExpiresAt().getTime() < System.currentTimeMillis()) {
        throw new WingsException(EXPIRED_TOKEN, USER_ADMIN);
      }
    } catch (Exception ex) {
      logger.warn("Error in verifying JWT token ", ex);
      throw ex instanceof JWTVerificationException ? new WingsException(INVALID_TOKEN) : new WingsException(ex);
    }
  }

  private void validateDelegateToken(String accountId, String tokenString) {
    logger.info("Delegate token validation, account id [{}] token requested", accountId);
    Account account = accountCache.get(Account.class, accountId);
    if (account == null) {
      logger.error("Account Id {} does not exist in manager. So, rejecting delegate register request.", accountId);
      throw new WingsException(ACCESS_DENIED);
    }

    EncryptedJWT encryptedJWT = null;
    try {
      encryptedJWT = EncryptedJWT.parse(tokenString);
    } catch (ParseException e) {
      logger.error("Invalid token for delegate " + tokenString, e);
      throw new WingsException(INVALID_TOKEN);
    }

    byte[] encodedKey;
    try {
      encodedKey = Hex.decodeHex(account.getAccountKey().toCharArray());
    } catch (DecoderException e) {
      logger.error("Invalid hex account key " + account.getAccountKey(), e);
      throw new WingsException(DEFAULT_ERROR_CODE); // ShouldNotHappen
    }

    JWEDecrypter decrypter;
    try {
      decrypter = new DirectDecrypter(new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES"));
    } catch (KeyLengthException e) {
      logger.error("Invalid account key " + account.getAccountKey(), e);
      throw new WingsException(DEFAULT_ERROR_CODE);
    }

    try {
      encryptedJWT.decrypt(decrypter);
    } catch (JOSEException e) {
      throw new WingsException(INVALID_TOKEN);
    }
  }
}
