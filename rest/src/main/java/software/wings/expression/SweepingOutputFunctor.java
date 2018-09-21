package software.wings.expression;

import lombok.Builder;
import software.wings.beans.SweepingOutput;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.utils.KryoUtils;

@Builder
public class SweepingOutputFunctor {
  private String appId;
  private String pipelineExecutionId;
  private String workflowExecutionId;
  private String phaseExecutionId;

  private SweepingOutputService sweepingOutputService;

  public Object output(String name) {
    SweepingOutput sweepingOutput =
        sweepingOutputService.find(appId, name, pipelineExecutionId, workflowExecutionId, phaseExecutionId);
    if (sweepingOutput == null) {
      return null;
    }
    return KryoUtils.asInflatedObject(sweepingOutput.getOutput());
  }
}
