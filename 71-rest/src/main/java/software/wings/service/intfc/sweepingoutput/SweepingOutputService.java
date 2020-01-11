package software.wings.service.intfc.sweepingoutput;

import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutputInstance;

import javax.validation.Valid;

public interface SweepingOutputService {
  SweepingOutputInstance save(@Valid SweepingOutputInstance sweepingOutputInstance);

  void ensure(@Valid SweepingOutputInstance sweepingOutputInstance);

  SweepingOutputInstance find(SweepingOutputInquiry inquiry);

  SweepingOutput findSweepingOutput(SweepingOutputInquiry inquiry);

  void copyOutputsForAnotherWorkflowExecution(
      String appId, String fromWorkflowExecutionId, String toWorkflowExecutionId);
}
