package software.wings.graphql.datafetcher.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.FeatureName.APP_TELEMETRY;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CgEventConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.service.EventConfigService;

import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.graphql.schema.query.QLEventsConfigsQueryParameters;
import software.wings.graphql.schema.type.event.QLEventsConfig;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDC)
public class PipelineEventsConfigConnectionDataFetcher
    extends AbstractArrayDataFetcher<QLEventsConfig, QLEventsConfigsQueryParameters> {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private EventConfigService eventConfigService;
  @Inject private AppService appService;

  @Override
  @AuthRule(permissionType = LOGGED_IN, action = PermissionAttribute.Action.READ)
  protected List<QLEventsConfig> fetch(QLEventsConfigsQueryParameters qlQueries, String accountId) {
    if (!featureFlagService.isEnabled(APP_TELEMETRY, accountId)) {
      throw new InvalidRequestException("Please enable feature flag to configure events");
    }
    if (!appService.exist(qlQueries.getAppId())) {
      throw new InvalidRequestException("Application does not exist");
    }
    List<CgEventConfig> cgEventConfigs =
        eventConfigService.listAllPipelineEventsConfig(accountId, qlQueries.getAppId());
    return cgEventConfigs.stream()
        .map(cgEventConfig -> QLEventsConfig.getQLEventsConfig(cgEventConfig))
        .collect(Collectors.toList());
  }
  @Override
  protected QLEventsConfig unusedReturnTypePassingDummyMethod() {
    return null;
  }
}
