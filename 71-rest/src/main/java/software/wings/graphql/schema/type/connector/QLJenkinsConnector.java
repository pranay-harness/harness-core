package software.wings.graphql.schema.type.connector;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLJenkinsConnectorKeys")
@Scope(ResourceType.SETTING)
public class QLJenkinsConnector implements QLConnector {
  private String id;
  private String name;
  private Long createdAt;
  private QLUser createdBy;

  public static class QLJenkinsConnectorBuilder implements QLConnectorBuilder {}
}
