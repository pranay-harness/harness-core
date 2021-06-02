package software.wings.graphql.schema.mutation.execution.input;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLResumeExecutionInputKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
public class QLResumeExecutionInput implements QLMutationInput {
  String applicationId;
  String pipelineExecutionId;
  String pipelineStageName;
  String clientMutationId;
}
