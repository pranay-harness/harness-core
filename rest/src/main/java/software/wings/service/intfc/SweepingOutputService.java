package software.wings.service.intfc;

import software.wings.beans.SweepingOutput;

import javax.validation.Valid;

public interface SweepingOutputService {
  SweepingOutput save(@Valid SweepingOutput sweepingOutput);

  SweepingOutput find(String appId, String pipelineExecutionId, String workflowExecutionId);
}
