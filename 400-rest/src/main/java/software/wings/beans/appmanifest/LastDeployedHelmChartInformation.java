package software.wings.beans.appmanifest;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.WorkflowType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class LastDeployedHelmChartInformation {
  HelmChart helmchart;
  Long executionStartTime;
  String envId;
  String executionId;
  String executionEntityId;
  WorkflowType executionEntityType;
  String executionEntityName;
}
