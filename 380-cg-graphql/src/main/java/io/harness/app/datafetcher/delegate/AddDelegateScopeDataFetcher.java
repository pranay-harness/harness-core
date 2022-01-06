/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.app.datafetcher.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.schema.mutation.delegate.input.QLAddDelegateScopeInput;
import io.harness.app.schema.mutation.delegate.payload.QLAddDelegateScopePayload;
import io.harness.app.schema.type.delegate.QLDelegateScope;
import io.harness.app.schema.type.delegate.QLDelegateScope.QLDelegateScopeBuilder;
import io.harness.delegate.beans.DelegateScope;
import io.harness.delegate.beans.DelegateScope.DelegateScopeBuilder;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.DelegateScopeService;

import com.google.inject.Inject;

@OwnedBy(DEL)
public class AddDelegateScopeDataFetcher
    extends BaseMutatorDataFetcher<QLAddDelegateScopeInput, QLAddDelegateScopePayload> {
  @Inject DelegateScopeService delegateScopeService;

  @Inject
  public AddDelegateScopeDataFetcher(DelegateScopeService delegateScopeService) {
    super(QLAddDelegateScopeInput.class, QLAddDelegateScopePayload.class);
    this.delegateScopeService = delegateScopeService;
  }

  @Override
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public QLAddDelegateScopePayload mutateAndFetch(QLAddDelegateScopeInput parameter, MutationContext mutationContext) {
    DelegateScopeBuilder delegateScopeBuilder = DelegateScope.builder();
    DelegateController.populateDelegateScope(mutationContext.getAccountId(), parameter, delegateScopeBuilder);
    DelegateScope scope = delegateScopeService.add(delegateScopeBuilder.build());
    if (scope == null) {
      return QLAddDelegateScopePayload.builder().message("Error while adding delegate scope").build();
    }
    QLDelegateScopeBuilder qlDelegateScopeBuilder = QLDelegateScope.builder();
    DelegateController.populateQLDelegateScope(scope, qlDelegateScopeBuilder);
    return QLAddDelegateScopePayload.builder()
        .message("Delegate Scope added")
        .delegateScope(qlDelegateScopeBuilder.build())
        .build();
  }
}
