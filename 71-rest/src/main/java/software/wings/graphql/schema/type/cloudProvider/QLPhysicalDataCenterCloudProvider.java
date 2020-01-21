package software.wings.graphql.schema.type.cloudProvider;

import io.harness.ccm.health.CEHealthStatus;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLPhysicalDataCenterConfigKeys")
@Scope(ResourceType.SETTING)
public class QLPhysicalDataCenterCloudProvider implements QLCloudProvider {
  private String id;
  private String name;
  private Long createdAt;
  private QLUser createdBy;
  private String type;
  private boolean isCloudCostEnabled;
  private CEHealthStatus ceHealthStatus;

  public static class QLPhysicalDataCenterCloudProviderBuilder implements QLCloudProviderBuilder {}
}
