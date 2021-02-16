package software.wings.graphql.schema.type;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLPipelineKeys")
@Scope(ResourceType.APPLICATION)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLPipeline implements QLObject {
  private String id;
  private String name;
  private String appId;
  private String description;
  private Long createdAt;
  private QLUser createdBy;
}
