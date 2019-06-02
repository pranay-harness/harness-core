package software.wings.graphql.schema.type.connector;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.time.OffsetDateTime;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLJiraConnectorKeys")
@Scope(ResourceType.SETTING)
public class QLJiraConnector implements QLConnector {
  private String id;
  private String name;
  private OffsetDateTime createdAt;
  private QLUser createdBy;

  public static class QLJiraConnectorBuilder implements QLConnectorBuilder {}
}
