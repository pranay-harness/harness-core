package io.harness.app.datafetcher.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.schema.mutation.delegate.input.QLAttachScopeToDelegateInput;
import io.harness.app.schema.mutation.delegate.payload.QLAttachScopeToDelegatePayload;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.service.intfc.DelegateCache;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(DEL)
public class AttachScopeToDelegateDataFetcher
    extends BaseMutatorDataFetcher<QLAttachScopeToDelegateInput, QLAttachScopeToDelegatePayload> {
  @Inject DelegateScopeService delegateScopeService;
  @Inject DelegateService delegateService;
  @Inject DelegateCache delegateCache;

  @Inject
  public AttachScopeToDelegateDataFetcher(
      DelegateScopeService delegateScopeService, DelegateService delegateService, DelegateCache delegateCache) {
    super(QLAttachScopeToDelegateInput.class, QLAttachScopeToDelegatePayload.class);
    this.delegateScopeService = delegateScopeService;
    this.delegateService = delegateService;
    this.delegateCache = delegateCache;
  }

  @Override
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public QLAttachScopeToDelegatePayload mutateAndFetch(
      QLAttachScopeToDelegateInput parameter, MutationContext mutationContext) {
    String delegateId = parameter.getDelegateId();
    String accountId = parameter.getAccountId();
    List<String> includeScopes = parameter.getIncludeScopes();
    List<String> excludeScopes = parameter.getExcludeScopes();

    if ((includeScopes == null && excludeScopes == null) || includeScopes.isEmpty() && excludeScopes.isEmpty()) {
      return QLAttachScopeToDelegatePayload.builder().message("No scopes to attach to delegate").build();
    }
    DelegateScopes delegateScopes =
        DelegateScopes.builder().includeScopeIds(includeScopes).excludeScopeIds(excludeScopes).build();

    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      Delegate delegate = delegateCache.get(accountId, delegateId, true);
      if (delegate == null) {
        return QLAttachScopeToDelegatePayload.builder()
            .message("Unable to fetch delegate with delegate id " + delegateId)
            .build();
      }
      if (includeScopes != null && !includeScopes.isEmpty()) {
        delegate.setIncludeScopes(delegateScopes.getIncludeScopeIds()
                                      .stream()
                                      .map(s -> delegateScopeService.get(accountId, s))
                                      .filter(Objects::nonNull)
                                      .collect(toList()));
      }
      if (excludeScopes != null && !excludeScopes.isEmpty()) {
        delegate.setExcludeScopes(delegateScopes.getExcludeScopeIds()
                                      .stream()
                                      .map(s -> delegateScopeService.get(accountId, s))
                                      .filter(Objects::nonNull)
                                      .collect(toList()));
      }
      delegateService.updateScopes(delegate);
      return QLAttachScopeToDelegatePayload.builder()
          .message("Scopes updated for delegate delegate id " + delegateId)
          .build();
    }
  }

  @Value
  @Builder
  protected static class DelegateScopes {
    List<String> includeScopeIds;
    List<String> excludeScopeIds;
  }
}
