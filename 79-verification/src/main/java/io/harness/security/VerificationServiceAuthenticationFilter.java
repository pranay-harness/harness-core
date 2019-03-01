package io.harness.security;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.mindrot.jbcrypt.BCrypt.checkpw;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import io.harness.beans.PageRequest;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.service.intfc.LearningEngineService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.dl.WingsPersistence;
import software.wings.security.annotations.CustomApiAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.PublicApi;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

@Singleton
@Priority(AUTHENTICATION)
public class VerificationServiceAuthenticationFilter implements ContainerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger(VerificationServiceAuthenticationFilter.class);
  @Context private ResourceInfo resourceInfo;
  @Inject private LearningEngineService learningEngineService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {
    if (authenticationExemptedRequests(containerRequestContext)) {
      return;
    }

    if (isCustomApiRequest(containerRequestContext)) {
      validateCustomApiRequest(containerRequestContext);
      return;
    }

    String authorization = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorization == null) {
      throw new WingsException(INVALID_TOKEN, USER);
    }

    if (isLearningEngineServiceRequest(containerRequestContext)) {
      validateLearningEngineServiceToken(
          substringAfter(containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "LearningEngine "));
      return;
    }

    throw new WingsException(INVALID_CREDENTIAL, USER);
  }

  private boolean isCustomApiRequest(ContainerRequestContext requestContext) {
    return customApi() && isNotEmpty(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION))
        && isNotEmpty(extractToken(requestContext, "Bearer"));
  }

  private boolean customApi() {
    return resourceInfo.getResourceClass().getAnnotation(CustomApiAuth.class) != null;
  }

  protected void validateCustomApiRequest(ContainerRequestContext containerRequestContext) {
    String tokenString = extractToken(containerRequestContext, "Bearer");
    if (isBlank(tokenString)) {
      throw new InvalidRequestException("Api Key not supplied", USER);
    }
    validate(tokenString, GLOBAL_ACCOUNT_ID);
  }

  public void validate(String key, String accountId) {
    PageRequest<ApiKeyEntry> pageRequest = aPageRequest().addFilter("accountId", EQ, accountId).build();
    if (!wingsPersistence.query(ApiKeyEntry.class, pageRequest)
             .getResponse()
             .stream()
             .map(apiKeyEntry -> checkpw(key, apiKeyEntry.getHashOfKey()))
             .collect(Collectors.toSet())
             .contains(true)) {
      throw new UnauthorizedException("Invalid Api Key", USER);
    }
  }

  protected String extractToken(ContainerRequestContext requestContext, String prefix) {
    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader == null || !authorizationHeader.startsWith(prefix)) {
      throw new WingsException(INVALID_TOKEN, USER_ADMIN);
    }
    return authorizationHeader.substring(prefix.length()).trim();
  }

  protected boolean authenticationExemptedRequests(ContainerRequestContext requestContext) {
    return requestContext.getMethod().equals(OPTIONS) || publicAPI()
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/version")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/swagger")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/swagger.json");
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
    String jwtLearningEngineServiceSecret = learningEngineService.getServiceSecretKey(ServiceType.LEARNING_ENGINE);
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
}
