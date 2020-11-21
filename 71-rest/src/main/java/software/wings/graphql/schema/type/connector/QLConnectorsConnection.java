package software.wings.graphql.schema.type.connector;

import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLConnectorsConnectionKeys")
@Scope(ResourceType.SETTING)
public class QLConnectorsConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLConnector> nodes;
}
