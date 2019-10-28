package software.wings.service.intfc;

import io.harness.beans.SweepingOutput;

import javax.validation.Valid;

public interface SweepingOutputService {
  SweepingOutput save(@Valid SweepingOutput sweepingOutput);

  void ensure(@Valid SweepingOutput sweepingOutput);

  SweepingOutput find(String appId, String name, String pipelineExecutionId, String workflowExecutionId,
      String phaseExecutionId, String stateExecutionId);
}
