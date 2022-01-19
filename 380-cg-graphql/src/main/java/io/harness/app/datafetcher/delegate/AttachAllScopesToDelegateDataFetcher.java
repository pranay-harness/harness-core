/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.app.datafetcher.delegate;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;

import static java.util.stream.Collectors.toList;

import io.harness.app.schema.mutation.delegate.input.QLAttachAllScopesToDelegateInput;
import io.harness.app.schema.mutation.delegate.payload.QLAttachScopeToDelegatePayload;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateScope;
import io.harness.delegate.beans.DelegateScope.DelegateScopeKeys;
import io.harness.service.intfc.DelegateCache;

import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AttachAllScopesToDelegateDataFetcher
    extends BaseMutatorDataFetcher<QLAttachAllScopesToDelegateInput, QLAttachScopeToDelegatePayload> {
  @Inject DelegateScopeService delegateScopeService;
  @Inject DelegateCache delegateCache;
  @Inject protected WingsPersistence persistence;
  @Inject DelegateService delegateService;

  @Inject
  public AttachAllScopesToDelegateDataFetcher(DelegateScopeService delegateScopeService, DelegateCache delegateCache) {
    super(QLAttachAllScopesToDelegateInput.class, QLAttachScopeToDelegatePayload.class);
    this.delegateScopeService = delegateScopeService;
    this.delegateCache = delegateCache;
  }

  @Override
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public QLAttachScopeToDelegatePayload mutateAndFetch(
      QLAttachAllScopesToDelegateInput parameter, MutationContext mutationContext) {
    String delegateId = parameter.getDelegateId();
    String accountId = parameter.getAccountId();
    Delegate delegate = delegateCache.get(accountId, delegateId, true);
    if (delegate == null) {
      return QLAttachScopeToDelegatePayload.builder()
          .message("Unable to fetch delegate with delegate id " + delegateId)
          .build();
    }

    List<DelegateScope> delegateScopes =
        persistence.createQuery(DelegateScope.class).filter(DelegateScopeKeys.accountId, accountId).asList();
    if (isEmpty(delegateScopes)) {
      return QLAttachScopeToDelegatePayload.builder()
          .message("No Scopes available in account to add to delegate.")
          .build();
    }
    delegate.setIncludeScopes(delegateScopes.stream().filter(Objects::nonNull).collect(toList()));
    delegateService.updateScopes(delegate);
    StringBuilder st = new StringBuilder();
    delegateScopes.forEach(scope -> st.append(scope.getName() + ", "));
    String responseString = "Included scopes for delegate:  " + st;
    return QLAttachScopeToDelegatePayload.builder().message(responseString).build();
  }
}
