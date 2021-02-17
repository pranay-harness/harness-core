package software.wings.graphql.schema.type;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutionConnectionKeys")
@Scope(ResourceType.APPLICATION)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLExecutionConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLExecution> nodes;
}
