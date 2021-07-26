package software.wings.graphql.datafetcher.event;

import static io.harness.beans.FeatureName.APP_TELEMETRY;

import static software.wings.graphql.schema.type.event.QLEventRule.toEventRule;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATIONS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.CgEventConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.service.EventConfigService;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.event.input.QLUpdateEventsConfigInput;
import software.wings.graphql.schema.mutation.event.payload.QLUpdateEventsConfigPayload;
import software.wings.graphql.schema.type.event.QLEventsConfig;
import software.wings.graphql.schema.type.event.QLWebhookEventConfig;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class UpdateEventsConfigDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateEventsConfigInput, QLUpdateEventsConfigPayload> {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private EventConfigService eventConfigService;

  public UpdateEventsConfigDataFetcher() {
    super(QLUpdateEventsConfigInput.class, QLUpdateEventsConfigPayload.class);
  }

  @Override
  @AuthRule(permissionType = MANAGE_APPLICATIONS, action = PermissionAttribute.Action.CREATE)
  protected QLUpdateEventsConfigPayload mutateAndFetch(
      QLUpdateEventsConfigInput parameter, MutationContext mutationContext) {
    String accountId = mutationContext.getAccountId();
    if (!featureFlagService.isEnabled(APP_TELEMETRY, mutationContext.getAccountId())) {
      throw new InvalidRequestException("Please enable feature flag to configure events");
    }
    CgEventConfig eventConfig = CgEventConfig.builder()
                                    .appId(parameter.getAppId())
                                    .accountId(accountId)
                                    .name(parameter.getName())
                                    .config(parameter.getWebhookConfig())
                                    .rule(parameter.getRule())
                                    .delegateSelectors(parameter.getDelegateSelectors())
                                    .enabled(parameter.isEnabled())
                                    .build();
    eventConfig.setUuid(parameter.getId());
    CgEventConfig updatedEventsConfig =
        eventConfigService.updateEventsConfig(accountId, parameter.getAppId(), eventConfig);
    return QLUpdateEventsConfigPayload.builder()
        .clientMutationId(parameter.getClientMutationId())
        .eventsConfig(QLEventsConfig.builder()
                          .appId(updatedEventsConfig.getAppId())
                          .name(updatedEventsConfig.getName())
                          .webhookConfig(QLWebhookEventConfig.toWebhookEventConfig(updatedEventsConfig.getConfig()))
                          .rule(toEventRule(updatedEventsConfig.getRule()))
                          .delegateSelectors(updatedEventsConfig.getDelegateSelectors())
                          .enabled(updatedEventsConfig.isEnabled())
                          .id(updatedEventsConfig.getUuid())
                          .build())
        .build();
  }
}
