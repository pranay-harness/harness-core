package software.wings.graphql.schema.type.cloudProvider;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.time.OffsetDateTime;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAzureConfigKeys")
@Scope(ResourceType.SETTING)
public class QLAzureCloudProvider implements QLCloudProvider {
  private String id;
  private String name;
  private String description;
  private OffsetDateTime createdAt;
  private QLUser createdBy;

  public static class QLAzureCloudProviderBuilder implements QLCloudProviderBuilder {}
}
