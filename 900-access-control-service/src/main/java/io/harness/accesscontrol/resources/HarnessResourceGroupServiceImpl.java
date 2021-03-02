package io.harness.accesscontrol.resources;

import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ACCOUNT_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ORG_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.PROJECT_LEVEL_PARAM_NAME;

import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeParams;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.exception.InvalidRequestException;
import io.harness.remote.client.NGRestUtils;
import io.harness.resourcegroupclient.ResourceGroupResponse;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.utils.RetryUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
public class HarnessResourceGroupServiceImpl implements HarnessResourceGroupService {
  private final ResourceGroupClient resourceGroupClient;
  private final ResourceGroupFactory resourceGroupFactory;
  private final ResourceGroupService resourceGroupService;
  private final ScopeParamsFactory scopeParamsFactory;

  private static final RetryPolicy<Object> retryPolicy =
      RetryUtils.getRetryPolicy("Could not find the resource group with the given identifier on attempt %s",
          "Could not find the resource group with the given identifier",
          Lists.newArrayList(InvalidRequestException.class), Duration.ofSeconds(5), 3, log);

  @Inject
  public HarnessResourceGroupServiceImpl(ResourceGroupClient resourceGroupClient,
      ResourceGroupFactory resourceGroupFactory, ResourceGroupService resourceGroupService, ScopeService scopeService,
      ScopeParamsFactory scopeParamsFactory) {
    this.resourceGroupClient = resourceGroupClient;
    this.resourceGroupFactory = resourceGroupFactory;
    this.resourceGroupService = resourceGroupService;
    this.scopeParamsFactory = scopeParamsFactory;
  }

  @Override
  public void sync(String identifier, Scope scope) {
    ScopeParams scopeParams = scopeParamsFactory.buildScopeParams(scope);

    ResourceGroupResponse resourceGroupResponse = Failsafe.with(retryPolicy).get(() -> {
      ResourceGroupResponse response = NGRestUtils.getResponse(resourceGroupClient.getResourceGroup(identifier,
          scopeParams.getParams().get(ACCOUNT_LEVEL_PARAM_NAME), scopeParams.getParams().get(ORG_LEVEL_PARAM_NAME),
          scopeParams.getParams().get(PROJECT_LEVEL_PARAM_NAME)));
      if (response == null || response.getResourceGroup() == null) {
        throw new InvalidRequestException(
            String.format("Resource group not found with the given identifier in scope %s", scope.toString()));
      }
      return response;
    });

    resourceGroupService.upsert(resourceGroupFactory.buildResourceGroup(resourceGroupResponse, scope.toString()));
  }

  @Override
  public void remove(String identifier, Scope scope) {
    resourceGroupService.delete(identifier, scope.toString());
  }
}
