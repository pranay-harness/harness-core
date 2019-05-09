package software.wings.graphql.schema.type.cloudProvider;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.time.ZonedDateTime;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLKubernetesClusterConfigKeys")
@Scope(ResourceType.SETTING)
public class QLKubernetesClusterCloudProvider implements QLCloudProvider {
  private String id;
  private String name;
  private String description;
  private ZonedDateTime createdAt;
  private QLUser createdBy;

  public static class QLKubernetesClusterCloudProviderBuilder implements QLCloudProviderBuilder {}
}
