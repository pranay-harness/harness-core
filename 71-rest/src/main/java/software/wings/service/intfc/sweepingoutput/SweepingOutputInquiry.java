package software.wings.service.intfc.sweepingoutput;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SweepingOutputInquiry {
  String appId;
  String name;
  String pipelineExecutionId;
  String workflowExecutionId;
  String phaseExecutionId;
  String stateExecutionId;
}
