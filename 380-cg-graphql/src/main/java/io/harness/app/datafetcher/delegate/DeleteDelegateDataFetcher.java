package software.wings.graphql.datafetcher.delegate;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.delegate.QLDeleteDelegateInput;
import software.wings.graphql.schema.mutation.delegate.QLDeleteDelegatePayload;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class DeleteDelegateDataFetcher extends BaseMutatorDataFetcher<QLDeleteDelegateInput, QLDeleteDelegatePayload> {
  @Inject private DelegateService delegateService;

  @Inject
  public DeleteDelegateDataFetcher(DelegateService delegateService) {
    super(QLDeleteDelegateInput.class, QLDeleteDelegatePayload.class);
    this.delegateService = delegateService;
  }

  @Override
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  @AuthRule(permissionType = MANAGE_DELEGATES)
  protected QLDeleteDelegatePayload mutateAndFetch(QLDeleteDelegateInput parameter, MutationContext mutationContext) {
    String accountId = parameter.getAccountId();
    String delegateId = parameter.getDelegateId();
    boolean forceDelete = parameter.isForceDelete();
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      delegateService.delete(accountId, delegateId, forceDelete);
    } catch (InvalidRequestException e) {
      log.error("Unable to complete request to delete delegate", e);
      return new QLDeleteDelegatePayload(
          mutationContext.getAccountId(), "Unable to complete request to delete delegate");
    }
    return new QLDeleteDelegatePayload(mutationContext.getAccountId(), "Delegate deleted");
  }
}
