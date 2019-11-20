package software.wings.service.intfc;

import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutputInstance;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;

public interface SweepingOutputService {
  SweepingOutputInstance save(@Valid SweepingOutputInstance sweepingOutputInstance);

  void ensure(@Valid SweepingOutputInstance sweepingOutputInstance);

  @Value
  @Builder
  class SweepingOutputInquiry {
    String appId;
    String name;
    String pipelineExecutionId;
    String workflowExecutionId;
    String phaseExecutionId;
    String stateExecutionId;
  }

  SweepingOutputInstance find(SweepingOutputInquiry inquiry);

  SweepingOutput findSweepingOutput(SweepingOutputInquiry inquiry);

  void copyOutputsForAnotherWorkflowExecution(
      String appId, String fromWorkflowExecutionId, String toWorkflowExecutionId);
}
