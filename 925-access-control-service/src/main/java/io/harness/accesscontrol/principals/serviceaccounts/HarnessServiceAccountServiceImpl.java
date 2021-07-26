package io.harness.accesscontrol.principals.serviceaccounts;

import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ACCOUNT_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ORG_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.PROJECT_LEVEL_PARAM_NAME;
import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeParams;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.serviceaccount.ServiceAccountDTO;
import io.harness.serviceaccount.remote.ServiceAccountClient;
import io.harness.utils.RetryUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class HarnessServiceAccountServiceImpl implements HarnessServiceAccountService {
  private final ScopeParamsFactory scopeParamsFactory;
  private final ServiceAccountService serviceAccountService;
  private final ServiceAccountClient serviceAccountClient;

  private static final RetryPolicy<Object> retryPolicy =
      RetryUtils.getRetryPolicy("Could not find the user with the given identifier on attempt %s",
          "Could not find the user with the given identifier", Lists.newArrayList(InvalidRequestException.class),
          Duration.ofSeconds(5), 3, log);

  @Inject
  public HarnessServiceAccountServiceImpl(ScopeParamsFactory scopeParamsFactory,
      ServiceAccountService serviceAccountService, @Named("PRIVILEGED") ServiceAccountClient serviceAccountClient) {
    this.scopeParamsFactory = scopeParamsFactory;
    this.serviceAccountService = serviceAccountService;
    this.serviceAccountClient = serviceAccountClient;
  }

  @Override
  public void sync(String identifier, Scope scope) {
    ScopeParams scopeParams = scopeParamsFactory.buildScopeParams(scope);
    List<String> resourceIds = new ArrayList<>();
    resourceIds.add(identifier);
    List<ServiceAccountDTO> serviceAccountDTOs = new ArrayList<>();

    serviceAccountDTOs.addAll(getResponse(serviceAccountClient.listServiceAccounts(
        scopeParams.getParams().get(ACCOUNT_LEVEL_PARAM_NAME), null, null, resourceIds)));

    serviceAccountDTOs.addAll(
        getResponse(serviceAccountClient.listServiceAccounts(scopeParams.getParams().get(ACCOUNT_LEVEL_PARAM_NAME),
            scopeParams.getParams().get(ORG_LEVEL_PARAM_NAME), null, resourceIds)));

    serviceAccountDTOs.addAll(getResponse(serviceAccountClient.listServiceAccounts(
        scopeParams.getParams().get(ACCOUNT_LEVEL_PARAM_NAME), scopeParams.getParams().get(ORG_LEVEL_PARAM_NAME),
        scopeParams.getParams().get(PROJECT_LEVEL_PARAM_NAME), resourceIds)));

    if (!serviceAccountDTOs.isEmpty()) {
      ServiceAccount serviceAccount =
          ServiceAccount.builder().identifier(identifier).scopeIdentifier(scope.toString()).build();
      serviceAccountService.createIfNotPresent(serviceAccount);
    } else {
      serviceAccountService.deleteIfPresent(identifier, scope.toString());
    }
  }
}
