package io.harness.app.datafetcher.delegate;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;

import io.harness.app.schema.query.delegate.QLDelegateQueryParameters;
import io.harness.app.schema.type.delegate.QLDelegateScope;
import io.harness.app.schema.type.delegate.QLDelegateScopeList;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateScope;

import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.DelegateScopeService;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class DelegateScopeListDataFetcher
    extends AbstractObjectDataFetcher<QLDelegateScopeList, QLDelegateQueryParameters> {
  @Inject DelegateScopeService delegateScopeService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLDelegateScopeList fetch(QLDelegateQueryParameters parameters, String accountId) {
    PageRequest<DelegateScope> pageRequest = aPageRequest()
                                                 .addFilter(DelegateScope.ACCOUNT_ID_KEY, EQ, parameters.getAccountId())
                                                 .withLimit(parameters.getLimit())
                                                 .build();
    PageResponse<DelegateScope> delegateScopes = delegateScopeService.list(pageRequest);

    List<QLDelegateScope> qlDelegateScopes =
        delegateScopes.stream().map(DelegateController::populateQLDelegateScope).collect(Collectors.toList());

    return QLDelegateScopeList.builder().nodes(qlDelegateScopes).build();
  }
}
