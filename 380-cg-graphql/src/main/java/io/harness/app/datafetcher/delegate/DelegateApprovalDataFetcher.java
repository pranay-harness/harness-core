package io.harness.app.datafetcher.delegate;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateApproval;

import io.harness.delegate.task.DelegateLogContext;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.jsonwebtoken.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import io.harness.app.schema.mutation.delegate.QLDelegateApproval;
import io.harness.app.schema.mutation.delegate.QLDelegateApprovalInput;
import io.harness.app.schema.mutation.delegate.QLDelegateApprovalPayload;
import io.harness.app.schema.type.delegate.QLDelegate;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;

@Slf4j
@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class DelegateApprovalDataFetcher
    extends BaseMutatorDataFetcher<QLDelegateApprovalInput, QLDelegateApprovalPayload> {
  @Inject DelegateService delegateService;

  @Inject
  public DelegateApprovalDataFetcher(DelegateService delegateService) {
    super(QLDelegateApprovalInput.class, QLDelegateApprovalPayload.class);
    this.delegateService = delegateService;
  }

  @Override
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public QLDelegateApprovalPayload mutateAndFetch(
      QLDelegateApprovalInput parameter, MutationContext mutationContext) {
    String delegateId = parameter.getDelegateId();
    String accountId = parameter.getAccountId();
    DelegateApproval delegateApproval = QLDelegateApproval.toDelegateApproval(parameter.getDelegateApproval());
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      Delegate delegate = delegateService.updateApprovalStatus(accountId, delegateId, delegateApproval);
      Assert.notNull(delegate, "Delegate Cannot be null");
      QLDelegate.QLDelegateBuilder qlDelegateBuilder = QLDelegate.builder();
      DelegateController.populateQLDelegate(delegate, qlDelegateBuilder);
      return new QLDelegateApprovalPayload(mutationContext.getAccountId(), qlDelegateBuilder.build());
    }
  }
}
