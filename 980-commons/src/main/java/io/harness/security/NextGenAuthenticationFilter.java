package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static javax.ws.rs.Priorities.AUTHENTICATION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.TokenDTO;
import io.harness.remote.client.NGRestUtils;
import io.harness.security.dto.Principal;
import io.harness.security.dto.ServiceAccountPrincipal;
import io.harness.token.remote.TokenClient;

import com.google.inject.Singleton;
import java.time.Instant;
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
  public static final String X_API_KEY = "X-Api-Key";
  private static final String deliminator = ".";

  private TokenClient tokenClient;

  public NextGenAuthenticationFilter(Predicate<Pair<ResourceInfo, ContainerRequestContext>> predicate,
      Map<String, JWTTokenHandler> serviceToJWTTokenHandlerMapping, Map<String, String> serviceToSecretMapping,
      TokenClient tokenClient) {
    super(predicate, serviceToJWTTokenHandlerMapping, serviceToSecretMapping);
    this.tokenClient = tokenClient;
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
      String[] splitToken = apiKey.split(deliminator);
      if (EmptyPredicate.isNotEmpty(splitToken) && splitToken.length == 2) {
        TokenDTO tokenDTO = NGRestUtils.getResponse(tokenClient.getToken(splitToken[0]));
        if (tokenDTO != null) {
          if (Instant.now().toEpochMilli() < tokenDTO.getValidTo()
              && Instant.now().toEpochMilli() > tokenDTO.getValidFrom()) {
            Principal principal = new ServiceAccountPrincipal(apiKey);
            SecurityContextBuilder.setContext(principal);
            SourcePrincipalContextBuilder.setSourcePrincipal(principal);
          } else {
            throw new InvalidRequestException("Incoming API token " + tokenDTO.getName() + " is expired");
          }
        } else {
          throw new InvalidRequestException("Could not find the incoming API token in Harness");
        }
      } else {
        throw new InvalidRequestException("Invalid incoming API token");
      }
    } else {
      super.filter(containerRequestContext);
    }
  }

  private Optional<String> getApiKeyFromHeaders(ContainerRequestContext containerRequestContext) {
    String apiKey = containerRequestContext.getHeaderString(X_API_KEY);
    return StringUtils.isEmpty(apiKey) ? Optional.empty() : Optional.of(apiKey);
  }
}
