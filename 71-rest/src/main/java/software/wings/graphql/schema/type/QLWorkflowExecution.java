package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTag;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWorkflowExecutionKeys")
@Scope(ResourceType.APPLICATION)
public class QLWorkflowExecution implements QLExecution {
  private String id;
  private Long createdAt;
  private Long startedAt;
  private Long endedAt;
  private QLExecutionStatus status;
  private QLCause cause;
  private String notes;
  private String appId;
  private List<QLDeploymentTag> tags;
}
