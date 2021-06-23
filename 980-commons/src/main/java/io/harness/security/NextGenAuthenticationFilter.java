package io.harness.security;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import static javax.ws.rs.Priorities.AUTHENTICATION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.dto.Principal;
import io.harness.security.dto.PrincipalType;
import io.harness.security.dto.ServiceAccountPrincipal;
import io.harness.security.dto.ServicePrincipal;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(PL)
@Singleton
@Priority(AUTHENTICATION)
@Slf4j
public class NextGenAuthenticationFilter extends JWTAuthenticationFilter {
  public static final String X_API_KEY = "x-api-key";

  public NextGenAuthenticationFilter(Predicate<Pair<ResourceInfo, ContainerRequestContext>> predicate,
      Map<String, JWTTokenHandler> serviceToJWTTokenHandlerMapping, Map<String, String> serviceToSecretMapping) {
    super(predicate, serviceToJWTTokenHandlerMapping, serviceToSecretMapping);
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) {
    if (!super.testRequestPredicate(containerRequestContext)) {
      // Predicate testing failed with the current request context
      return;
    }

    Optional<String> apiKeyOptional = getApiKeyFromHeaders(containerRequestContext);
    if (apiKeyOptional.isPresent()) {
      String apiKey = apiKeyOptional.get();
      log.info("Found an API key in request {}", apiKey);
      Principal principal = new ServiceAccountPrincipal(apiKey);
      SecurityContextBuilder.setContext(principal);
      SourcePrincipalContextBuilder.setSourcePrincipal(principal);
    } else {
      super.filter(containerRequestContext);
    }
  }

  private Optional<String> getApiKeyFromHeaders(ContainerRequestContext containerRequestContext) {
    String apiKey = containerRequestContext.getHeaderString(X_API_KEY);
    return StringUtils.isEmpty(apiKey) ? Optional.empty() : Optional.of(apiKey);
  }
}
