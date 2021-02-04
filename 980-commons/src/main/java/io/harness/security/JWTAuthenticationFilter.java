package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.exception.WingsException.USER;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.SPACE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import com.auth0.jwt.interfaces.Claim;
import java.util.Map;
import java.util.function.Predicate;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(PL)
public abstract class JWTAuthenticationFilter implements ContainerRequestFilter, ContainerResponseFilter {
  @Context private ResourceInfo resourceInfo;
  private final Predicate<Pair<ResourceInfo, ContainerRequestContext> > predicate;
  private final Map<String, JWTTokenHandler> serviceToJWTTokenHandlerMapping;
  private final Map<String, String> serviceToSecretMapping;

  public JWTAuthenticationFilter(Predicate<Pair<ResourceInfo, ContainerRequestContext> > predicate,
      Map<String, JWTTokenHandler> serviceToJWTTokenHandlerMapping, Map<String, String> serviceToSecretMapping) {
    this.predicate = predicate;
    this.serviceToJWTTokenHandlerMapping =
        serviceToJWTTokenHandlerMapping == null ? emptyMap() : serviceToJWTTokenHandlerMapping;
    this.serviceToSecretMapping = serviceToSecretMapping;
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) {
    if (predicate.test(Pair.of(resourceInfo, containerRequestContext))) {
      filter(containerRequestContext, serviceToJWTTokenHandlerMapping, serviceToSecretMapping);
    }
  }

  public static void filter(ContainerRequestContext containerRequestContext,
      Map<String, JWTTokenHandler> serviceToJWTTokenHandlerMapping, Map<String, String> serviceToSecretMapping) {
    String sourceServiceId = JWTTokenServiceUtils.extractSource(containerRequestContext);
    String secret = JWTTokenServiceUtils.extractSecret(serviceToSecretMapping, sourceServiceId);
    String token = JWTTokenServiceUtils.extractToken(containerRequestContext, sourceServiceId + SPACE);
    Pair<Boolean, Map<String, Claim> > validate =
        serviceToJWTTokenHandlerMapping.getOrDefault(sourceServiceId, JWTTokenServiceUtils::isServiceAuthorizationValid)
            .validate(token, secret);
    if (Boolean.TRUE.equals(validate.getLeft())) {
      SecurityContextBuilder.setContext(validate.getRight());
      return;
    }
    throw new InvalidRequestException(INVALID_TOKEN.name(), INVALID_TOKEN, USER);
  }

  @Override
  public void filter(
      ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
    if (predicate.test(Pair.of(resourceInfo, containerRequestContext))) {
      SecurityContextBuilder.unsetContext();
    }
  }
}
