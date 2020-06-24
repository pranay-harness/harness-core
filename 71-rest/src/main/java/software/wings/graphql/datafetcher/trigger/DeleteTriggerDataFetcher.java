package software.wings.graphql.datafetcher.trigger;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.type.trigger.QLDeleteTriggerInput;
import software.wings.graphql.schema.type.trigger.QLDeleteTriggerPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.TriggerService;

public class DeleteTriggerDataFetcher extends BaseMutatorDataFetcher<QLDeleteTriggerInput, QLDeleteTriggerPayload> {
  TriggerService triggerService;
  AppService appService;

  @Inject
  public DeleteTriggerDataFetcher(TriggerService triggerService, AppService appService) {
    super(QLDeleteTriggerInput.class, QLDeleteTriggerPayload.class);
    this.triggerService = triggerService;
    this.appService = appService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLDeleteTriggerPayload mutateAndFetch(QLDeleteTriggerInput parameter, MutationContext mutationContext) {
    String appId = parameter.getApplicationId();
    String triggerId = parameter.getTriggerId();

    if (EmptyPredicate.isEmpty(appId)) {
      throw new InvalidRequestException("ApplicationId must not be empty", USER);
    }

    if (!mutationContext.getAccountId().equals(appService.getAccountIdByAppId(appId))) {
      throw new InvalidRequestException("ApplicationId doesn't belong to this account", USER);
    }

    Trigger trigger = triggerService.get(appId, triggerId);
    if (trigger != null) {
      if (triggerService.triggerActionExists(trigger)) {
        triggerService.authorize(trigger, true);
      }
      triggerService.delete(appId, triggerId);

      if (triggerService.get(appId, triggerId) != null) {
        throw new InvalidRequestException("Trigger is not deleted", USER);
      }
    }

    return QLDeleteTriggerPayload.builder().clientMutationId(parameter.getClientMutationId()).build();
  }
}
