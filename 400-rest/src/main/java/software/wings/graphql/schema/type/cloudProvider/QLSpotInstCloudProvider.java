package software.wings.graphql.schema.type.cloudProvider;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.health.CEHealthStatus;

import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSpotInstConfigKeys")
@Scope(ResourceType.SETTING)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLSpotInstCloudProvider implements QLCloudProvider {
  private String id;
  private String name;
  private String description;
  private Long createdAt;
  private QLUser createdBy;
  private String type;
  private boolean isContinuousEfficiencyEnabled;
  private CEHealthStatus ceHealthStatus;

  public static class QLSpotInstCloudProviderBuilder implements QLCloudProviderBuilder {}
}
