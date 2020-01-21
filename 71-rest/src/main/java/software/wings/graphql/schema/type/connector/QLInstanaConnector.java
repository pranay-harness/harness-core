package software.wings.graphql.schema.type.connector;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(ResourceType.SETTING)
public class QLInstanaConnector implements QLConnector {
  private String id;
  private String name;
  private Long createdAt;
  private QLUser createdBy;

  public static class QLInstanaConnectorBuilder implements QLConnectorBuilder {}
}
