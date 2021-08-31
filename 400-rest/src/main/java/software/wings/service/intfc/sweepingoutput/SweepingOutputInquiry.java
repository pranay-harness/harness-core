package software.wings.service.intfc.sweepingoutput;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class SweepingOutputInquiry {
  String appId;
  String name;
  String pipelineExecutionId;
  String workflowExecutionId;
  String phaseExecutionId;
  String stateExecutionId;
}
