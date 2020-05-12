package software.wings.graphql.schema.type.connector;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLBambooConnectorKeys")
@Scope(ResourceType.SETTING)
public class QLBambooConnector implements QLConnector {
  private String id;
  private String name;
  private Long createdAt;
  private QLUser createdBy;

  public static class QLBambooConnectorBuilder implements QLConnectorBuilder {}
}
