package io.harness.accesscontrol.principals.serviceaccounts;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.common.validation.ValidationResult;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.principals.PrincipalValidator;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.utils.RetryUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

@OwnedBy(PL)
@Slf4j
@Singleton
public class ServiceAccountValidator implements PrincipalValidator {
  private final ServiceAccountService serviceAccountService;

  private static final RetryPolicy<Object> retryPolicy =
      RetryUtils.getRetryPolicy("Could not find the service account with the given identifier on attempt %s",
          "Could not find the service account with the given identifier",
          Lists.newArrayList(InvalidRequestException.class), Duration.ofSeconds(5), 3, log);

  @Inject
  public ServiceAccountValidator(ServiceAccountService serviceAccountService) {
    this.serviceAccountService = serviceAccountService;
  }

  @Override
  public PrincipalType getPrincipalType() {
    return USER;
  }

  @Override
  public ValidationResult validatePrincipal(Principal principal, String scopeIdentifier) {
    String identifier = principal.getPrincipalIdentifier();
    Optional<ServiceAccount> serviceAccountOptional = serviceAccountService.get(identifier, scopeIdentifier);
    if (serviceAccountOptional.isPresent()) {
      return ValidationResult.builder().valid(true).build();
    }
    return ValidationResult.builder()
        .valid(false)
        .errorMessage(String.format(
            "service account not found with the given identifier %s in the scope %s", identifier, scopeIdentifier))
        .build();
  }
}
