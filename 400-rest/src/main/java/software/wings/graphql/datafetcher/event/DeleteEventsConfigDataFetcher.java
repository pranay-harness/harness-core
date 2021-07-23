package software.wings.graphql.datafetcher.event;

import static io.harness.beans.FeatureName.APP_TELEMETRY;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATIONS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.service.EventConfigService;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.event.input.QLDeleteEventsConfigInput;
import software.wings.graphql.schema.mutation.event.payload.QLDeleteEventsConfigPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class DeleteEventsConfigDataFetcher
    extends BaseMutatorDataFetcher<QLDeleteEventsConfigInput, QLDeleteEventsConfigPayload> {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private EventConfigService eventConfigService;

  public DeleteEventsConfigDataFetcher() {
    super(QLDeleteEventsConfigInput.class, QLDeleteEventsConfigPayload.class);
  }

  @Override
  @AuthRule(permissionType = MANAGE_APPLICATIONS, action = PermissionAttribute.Action.CREATE)
  protected QLDeleteEventsConfigPayload mutateAndFetch(
      QLDeleteEventsConfigInput parameter, MutationContext mutationContext) {
    String accountId = mutationContext.getAccountId();
    if (!featureFlagService.isEnabled(APP_TELEMETRY, mutationContext.getAccountId())) {
      throw new InvalidRequestException("Please enable feature flag to configure events");
    }
    eventConfigService.deleteEventsConfig(accountId, parameter.getAppId(), parameter.getEventsConfigId());
    return QLDeleteEventsConfigPayload.builder().clientMutationId(parameter.getClientMutationId()).build();
  }
}
