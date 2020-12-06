package software.wings.graphql.schema.type.connector;

import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLCustomConnector implements QLConnector {
  private String id;
  private String name;
  private Long createdAt;
  private QLUser createdBy;

  public static class QLCustomConnectorBuilder implements QLConnectorBuilder {}
}
