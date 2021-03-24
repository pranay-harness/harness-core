package io.harness.accesscontrol.principals.user;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.common.validation.ValidationResult;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.principals.PrincipalValidator;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.user.User;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.remote.client.RestClientUtils;
import io.harness.utils.RetryUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@OwnedBy(PL)
@Slf4j
@Singleton
public class UserValidator implements PrincipalValidator {
  private final UserClient userClient;

  private static final RetryPolicy<Object> retryPolicy =
      RetryUtils.getRetryPolicy("Could not find the user with the given identifier on attempt %s",
          "Could not find the user with the given identifier", Lists.newArrayList(InvalidRequestException.class),
          Duration.ofSeconds(5), 3, log);

  @Inject
  public UserValidator(UserClient userClient) {
    this.userClient = userClient;
  }

  @Override
  public PrincipalType getPrincipalType() {
    return USER;
  }

  @Override
  public ValidationResult validatePrincipal(Principal principal, String scopeIdentifier) {
    String userId = principal.getPrincipalIdentifier();
    return Failsafe.with(retryPolicy).get(() -> {
      Optional<User> userOptional =
          RestClientUtils.getResponse(userClient.getUsersByIds(Lists.newArrayList(userId))).stream().findFirst();
      if (!userOptional.isPresent()) {
        return ValidationResult.builder()
            .valid(false)
            .errorMessage(String.format("user not found with the given identifier %s", userId))
            .build();
      }
      return ValidationResult.builder().valid(true).build();
    });
  }
}
