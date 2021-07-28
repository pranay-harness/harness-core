package software.wings.graphql.schema.type.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLEventsConfig {
  String id;
  private String name;
  private QLWebhookEventConfig webhookConfig;
  private QLEventRule rule;
  private List<String> delegateSelectors;
  private boolean enabled;
  private String appId;
}
