package software.wings.graphql.schema.type.connector;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@OwnedBy(CDC)
@Value
@Builder
@Scope(ResourceType.SETTING)
public class QLSftpConnector implements QLConnector {
  private String id;
  private String name;
  private Long createdAt;
  private QLUser createdBy;

  public static class QLSftpConnectorBuilder implements QLConnectorBuilder {}
}
