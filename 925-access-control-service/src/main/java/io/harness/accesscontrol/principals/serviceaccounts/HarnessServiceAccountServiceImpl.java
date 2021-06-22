package io.harness.accesscontrol.principals.serviceaccounts;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeParams;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.utils.RetryUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class HarnessServiceAccountServiceImpl implements HarnessServiceAccountService {
  private final ScopeParamsFactory scopeParamsFactory;
  private final ServiceAccountService serviceAccountService;

  private static final RetryPolicy<Object> retryPolicy =
      RetryUtils.getRetryPolicy("Could not find the user with the given identifier on attempt %s",
          "Could not find the user with the given identifier", Lists.newArrayList(InvalidRequestException.class),
          Duration.ofSeconds(5), 3, log);

  @Inject
  public HarnessServiceAccountServiceImpl(
      ScopeParamsFactory scopeParamsFactory, ServiceAccountService serviceAccountService) {
    this.scopeParamsFactory = scopeParamsFactory;
    this.serviceAccountService = serviceAccountService;
  }

  @Override
  public void sync(String identifier, Scope scope) {
    ScopeParams scopeParams = scopeParamsFactory.buildScopeParams(scope);
    Boolean isUserInScope = Boolean.TRUE;
    if (Boolean.TRUE.equals(isUserInScope)) {
      ServiceAccount serviceAccount =
          ServiceAccount.builder().identifier(identifier).scopeIdentifier(scope.toString()).build();
      serviceAccountService.createIfNotPresent(serviceAccount);
    } else {
      serviceAccountService.deleteIfPresent(identifier, scope.toString());
    }
  }
}
