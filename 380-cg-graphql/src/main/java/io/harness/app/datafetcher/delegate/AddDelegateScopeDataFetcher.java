package io.harness.app.datafetcher.delegate;

import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateScope;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import io.harness.app.schema.mutation.delegate.QLAddDelegateScopeInput;
import io.harness.app.schema.mutation.delegate.QLAddDelegateScopePayload;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.DelegateScopeService;


import com.google.inject.Inject;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class AddDelegateScopeDataFetcher
    extends BaseMutatorDataFetcher<QLAddDelegateScopeInput, QLAddDelegateScopePayload> {
  @Inject DelegateScopeService delegateScopeService;

  @Inject
  public AddDelegateScopeDataFetcher(DelegateScopeService delegateScopeService) {
    super(QLAddDelegateScopeInput.class, QLAddDelegateScopePayload.class);
    this.delegateScopeService = delegateScopeService;
  }

  @Override
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public QLAddDelegateScopePayload mutateAndFetch(
      QLAddDelegateScopeInput parameter, MutationContext mutationContext) {
    DelegateScope.DelegateScopeBuilder delegateScopeBuilder = DelegateScope.builder();
    DelegateController.populateDelegateScope(parameter.getDelegateScope(), delegateScopeBuilder);
    DelegateScope scope = delegateScopeService.add(delegateScopeBuilder.build());
    if (scope == null) {
      return QLAddDelegateScopePayload.builder().message("Error while adding delegate scope").build();
    }
    return QLAddDelegateScopePayload.builder().message("Delegate Scope added").build();
  }
}
