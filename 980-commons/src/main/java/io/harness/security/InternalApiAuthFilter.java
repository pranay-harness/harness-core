package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static javax.ws.rs.Priorities.AUTHENTICATION;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(PL)
@Singleton
@Priority(AUTHENTICATION)
public class InternalApiAuthFilter extends JWTAuthenticationFilter {
  public InternalApiAuthFilter(Predicate<Pair<ResourceInfo, ContainerRequestContext>> predicate,
      Map<String, JWTTokenHandler> serviceToJWTTokenHandlerMapping, Map<String, String> serviceToSecretMapping) {
    super(predicate, serviceToJWTTokenHandlerMapping, serviceToSecretMapping);
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) {
    if (!super.testRequestPredicate(containerRequestContext)) {
      // Predicate testing failed with the current request context
      return;
    }
    super.filter(containerRequestContext);
  }
}
