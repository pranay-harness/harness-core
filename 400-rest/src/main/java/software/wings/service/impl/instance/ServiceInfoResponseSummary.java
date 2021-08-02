package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;

@Value
@Builder
@OwnedBy(DX)
@FieldNameConstants(innerTypeName = "ServiceInfoResponseSummaryKeys")
public class ServiceInfoResponseSummary {
  String lastArtifactBuildNum;
  String lastWorkflowExecutionId;
  String lastWorkflowExecutionName;
  String infraMappingId;
  String infraMappingName;
}
