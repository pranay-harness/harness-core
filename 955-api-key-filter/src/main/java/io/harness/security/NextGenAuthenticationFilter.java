package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.token.TokenClientModule.NG_HARNESS_API_KEY_CACHE;

import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.BCryptVersion.$2A;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.common.beans.ApiKeyType;
import io.harness.ng.core.dto.TokenDTO;
import io.harness.remote.client.NGRestUtils;
import io.harness.security.dto.Principal;
import io.harness.security.dto.ServiceAccountPrincipal;
import io.harness.security.dto.UserPrincipal;
import io.harness.token.remote.TokenClient;

import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Priority;
import javax.cache.Cache;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@OwnedBy(PL)
@Singleton
@Priority(AUTHENTICATION)
@Slf4j
public class NextGenAuthenticationFilter extends JWTAuthenticationFilter {
  public static final String X_API_KEY = "X-Api-Key";
  private static final String delimiter = "\\.";

  private final TokenClient tokenClient;
  private final Cache<String, TokenDTO> tokenCache;

  public NextGenAuthenticationFilter(Predicate<Pair<ResourceInfo, ContainerRequestContext>> predicate,
      Map<String, JWTTokenHandler> serviceToJWTTokenHandlerMapping, Map<String, String> serviceToSecretMapping,
      @Named("PRIVILEGED") TokenClient tokenClient,
      @Named(NG_HARNESS_API_KEY_CACHE) Cache<String, TokenDTO> tokenCache) {
    super(predicate, serviceToJWTTokenHandlerMapping, serviceToSecretMapping);
    this.tokenClient = tokenClient;
    this.tokenCache = tokenCache;
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
      String[] splitToken = apiKey.split(delimiter);
      String tokenId = splitToken[0];
      if (EmptyPredicate.isNotEmpty(splitToken)) {
        TokenDTO tokenDTO = getTokenDTO(tokenId);
        String rawPassword = apiKey.replaceAll(tokenId + delimiter, "");
        if (tokenDTO != null) {
          if (!new BCryptPasswordEncoder($2A, 10).matches(rawPassword, tokenDTO.getIdentifier())) {
            throw new InvalidRequestException("Could not find the API token in Harness");
          }
          if (!tokenDTO.isValid()) {
            // @Todo(Raj): Should we remove the entry from cache?
            //             What if a user keeps on hitting with expired token? We should hit the cache and return
            // removeTokenFromCache(tokenId);
            throw new InvalidRequestException("Incoming API token " + tokenDTO.getName() + " has expired");
          }

          Principal principal = null;
          if (tokenDTO.getApiKeyType() == ApiKeyType.SERVICE_ACCOUNT) {
            principal = new ServiceAccountPrincipal(tokenDTO.getParentIdentifier());
          } else if (tokenDTO.getApiKeyType() == ApiKeyType.USER) {
            principal = new UserPrincipal(tokenDTO.getParentIdentifier(), tokenDTO.getEmail(), tokenDTO.getUsername(),
                tokenDTO.getAccountIdentifier());
          }

          SecurityContextBuilder.setContext(principal);
          SourcePrincipalContextBuilder.setSourcePrincipal(principal);

          storeTokenInCache(tokenId, tokenDTO);
        } else {
          throw new InvalidRequestException("Could not find the API token in Harness");
        }
      } else {
        throw new InvalidRequestException("Invalid API token");
      }
    } else {
      super.filter(containerRequestContext);
    }
  }

  private TokenDTO getTokenDTO(String tokenId) {
    TokenDTO tokenDTO = getTokenFromCache(tokenId);
    if (tokenDTO != null) {
      log.info("apikey cache hit for {}: {}", tokenId, tokenDTO);
      return tokenDTO;
    } else {
      tokenDTO = NGRestUtils.getResponse(tokenClient.getToken(tokenId));
    }

    return tokenDTO;
  }

  private TokenDTO getTokenFromCache(String tokenId) {
    try {
      return tokenCache.get(tokenId);
    } catch (Exception e) {
      log.error("Exception while reading from apikey token cache, fetching from http call. tokenId: " + tokenId, e);
      return null;
    }
  }

  private void removeTokenFromCache(String tokenId) {
    try {
      tokenCache.remove(tokenId);
    } catch (Exception e) {
      log.error("Exception while removing apikey token cache. tokenId: " + tokenId, e);
    }
  }

  private void storeTokenInCache(String tokenId, TokenDTO tokenDTO) {
    try {
      tokenCache.put(tokenId, tokenDTO);
    } catch (Exception e) {
      log.error("Exception while storing apikey token in cache. tokenId: " + tokenId, e);
    }
  }

  private Optional<String> getApiKeyFromHeaders(ContainerRequestContext containerRequestContext) {
    String apiKey = containerRequestContext.getHeaderString(X_API_KEY);
    return StringUtils.isEmpty(apiKey) ? Optional.empty() : Optional.of(apiKey);
  }
}