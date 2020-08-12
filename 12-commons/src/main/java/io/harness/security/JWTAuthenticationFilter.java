package io.harness.security;

import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.exception.WingsException.USER;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.SPACE;

import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.function.Predicate;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;

@Singleton
public class JWTAuthenticationFilter implements ContainerRequestFilter {
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
      String sourceServiceId = JWTTokenServiceUtils.extractSource(containerRequestContext);
      String secret = JWTTokenServiceUtils.extractSecret(serviceToSecretMapping, sourceServiceId);
      String token = JWTTokenServiceUtils.extractToken(containerRequestContext, sourceServiceId + SPACE);
      boolean validate = serviceToJWTTokenHandlerMapping
                             .getOrDefault(sourceServiceId, JWTTokenServiceUtils::isServiceAuthorizationValid)
                             .validate(token, secret);
      if (validate) {
        return;
      }
      throw new InvalidRequestException(INVALID_TOKEN.name(), INVALID_TOKEN, USER);
    }
  }
}
