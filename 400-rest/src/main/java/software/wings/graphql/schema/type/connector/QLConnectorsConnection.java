package software.wings.graphql.schema.type.connector;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

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
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLConnectorsConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLConnector> nodes;
}
