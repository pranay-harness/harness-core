package io.harness.engine.pms.resume.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.plan.Node;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;

import com.google.protobuf.ByteString;
import lombok.Builder;
import lombok.Data;

/**
 * Update NodeProjectUtils#fieldsForResume if any new field is added/removed from here
 */
@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class ResumeMetadata {
  String nodeExecutionUuid;
  Node planNode;
  Ambiance ambiance;
  ExecutionMode mode;
  ByteString resolvedStepParameters;
  ExecutableResponse latestExecutableResponse;

  public static ResumeMetadata fromNodeExecution(NodeExecution nodeExecution) {
    return ResumeMetadata.builder()
        .nodeExecutionUuid(nodeExecution.getUuid())
        .planNode(nodeExecution.getNode())
        .ambiance(nodeExecution.getAmbiance())
        .mode(nodeExecution.getMode())
        .resolvedStepParameters(nodeExecution.getResolvedStepParametersBytes())
        .latestExecutableResponse(nodeExecution.obtainLatestExecutableResponse())
        .build();
  }
}
