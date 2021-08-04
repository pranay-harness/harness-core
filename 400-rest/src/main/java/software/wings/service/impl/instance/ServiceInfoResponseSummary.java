package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class ServiceInfoResponseSummary {
  String lastArtifactBuildNum;
  String lastWorkflowExecutionId;
  String lastWorkflowExecutionName;
  String infraMappingId;
  String infraMappingName;
}
