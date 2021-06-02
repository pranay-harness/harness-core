package io.harness.accesscontrol.principals.user;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ACCOUNT_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ORG_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.PROJECT_LEVEL_PARAM_NAME;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.common.validation.ValidationResult;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.principals.PrincipalValidator;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeParams;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.remote.client.NGRestUtils;
import io.harness.usermembership.remote.UserMembershipClient;
import io.harness.utils.RetryUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@OwnedBy(PL)
@Slf4j
@Singleton
public class UserValidator implements PrincipalValidator {
  private final UserMembershipClient userMembershipClient;
  private final ScopeParamsFactory scopeParamsFactory;
  private final ScopeService scopeService;

  private static final RetryPolicy<Object> retryPolicy =
      RetryUtils.getRetryPolicy("Could not find the user with the given identifier on attempt %s",
          "Could not find the user with the given identifier", Lists.newArrayList(InvalidRequestException.class),
          Duration.ofSeconds(5), 3, log);

  @Inject
  public UserValidator(
      UserMembershipClient userMembershipClient, ScopeParamsFactory scopeParamsFactory, ScopeService scopeService) {
    this.userMembershipClient = userMembershipClient;
    this.scopeParamsFactory = scopeParamsFactory;
    this.scopeService = scopeService;
  }

  @Override
  public PrincipalType getPrincipalType() {
    return USER;
  }

  @Override
  public ValidationResult validatePrincipal(Principal principal, String scopeIdentifier) {
    String userId = principal.getPrincipalIdentifier();
    Scope scope = scopeService.buildScopeFromScopeIdentifier(scopeIdentifier);
    ScopeParams scopeParams = scopeParamsFactory.buildScopeParams(scope);
    return Failsafe.with(retryPolicy).get(() -> {
      Boolean isUserInScope = NGRestUtils.getResponse(userMembershipClient.isUserInScope(userId,
          scopeParams.getParams().get(ACCOUNT_LEVEL_PARAM_NAME), scopeParams.getParams().get(ORG_LEVEL_PARAM_NAME),
          scopeParams.getParams().get(PROJECT_LEVEL_PARAM_NAME)));
      if (!Boolean.TRUE.equals(isUserInScope)) {
        return ValidationResult.builder()
            .valid(false)
            .errorMessage(
                String.format("user %s is not added as a collaborator in the scope %s", userId, scopeIdentifier))
            .build();
      }
      return ValidationResult.builder().valid(true).build();
    });
  }
}
