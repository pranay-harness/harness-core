package software.wings.graphql.datafetcher.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.FeatureName.APP_TELEMETRY;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.CgEventConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ff.FeatureFlagService;
import io.harness.service.EventConfigService;

import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLEventsConfigQueryParameters;
import software.wings.graphql.schema.type.event.QLEventsConfig;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@OwnedBy(CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class EventsConfigDataFetcher extends AbstractObjectDataFetcher<QLEventsConfig, QLEventsConfigQueryParameters> {
  @Inject EventConfigService eventConfigService;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  @AuthRule(permissionType = LOGGED_IN, action = PermissionAttribute.Action.READ)
  protected QLEventsConfig fetch(QLEventsConfigQueryParameters qlQuery, String accountId) {
    if (!featureFlagService.isEnabled(APP_TELEMETRY, accountId)) {
      throw new InvalidRequestException("Please enable feature flag to configure events");
    }
    CgEventConfig cgEventConfig = null;
    if (StringUtils.isNotBlank(qlQuery.getEventsConfigId())) {
      cgEventConfig = eventConfigService.getEventsConfig(accountId, qlQuery.getAppId(), qlQuery.getEventsConfigId());
    }
    if (StringUtils.isNotBlank(qlQuery.getName())) {
      cgEventConfig = eventConfigService.getEventsConfigByName(accountId, qlQuery.getAppId(), qlQuery.getName());
    }
    QLEventsConfig qlEventsConfig = QLEventsConfig.getQLEventsConfig(cgEventConfig);
    if (qlEventsConfig == null) {
      throw new InvalidRequestException("Events Config Does Not Exist", WingsException.USER);
    }
    return qlEventsConfig;
  }
}
