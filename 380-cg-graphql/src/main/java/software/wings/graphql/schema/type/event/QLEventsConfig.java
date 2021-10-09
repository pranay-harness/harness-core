package software.wings.graphql.schema.type.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.graphql.schema.type.event.QLEventRule.toEventRule;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CgEventConfig;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLEventsConfig {
  String id;
  private String name;
  private QLWebhookEventConfig webhookConfig;
  private QLEventRule rule;
  private List<String> delegateSelectors;
  private boolean enabled;
  private String appId;

  public static QLEventsConfig getQLEventsConfig(CgEventConfig cgEventConfig) {
    if (cgEventConfig == null) {
      return null;
    }
    QLEventsConfigBuilder builder = QLEventsConfig.builder();
    return builder.appId(cgEventConfig.getAppId())
        .name(cgEventConfig.getName())
        .webhookConfig(QLWebhookEventConfig.toWebhookEventConfig(cgEventConfig.getConfig()))
        .rule(toEventRule(cgEventConfig.getRule()))
        .delegateSelectors(cgEventConfig.getDelegateSelectors())
        .enabled(cgEventConfig.isEnabled())
        .id(cgEventConfig.getUuid())
        .build();
  }
}
