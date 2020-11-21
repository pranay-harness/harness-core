package software.wings.graphql.schema.mutation.pipeline.payload;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.PipelineExecution;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CDC)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
public class QLContinueExecutionPayload {
  private String clientMutationId;
  private boolean status;
  private PipelineExecution pipelineExecution;
}
