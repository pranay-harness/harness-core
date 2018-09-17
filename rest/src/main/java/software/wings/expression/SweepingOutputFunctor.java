package software.wings.expression;

import lombok.Builder;
import software.wings.beans.SweepingOutput;
import software.wings.service.intfc.SweepingOutputService;

import java.util.Map;

@Builder
public class SweepingOutputFunctor {
  private String appId;
  private String pipelineExecutionId;
  private String workflowExecutionId;

  private SweepingOutputService sweepingOutputService;

  public Map<String, Object> output(String key) {
    SweepingOutput sweepingOutput = sweepingOutputService.find(appId, pipelineExecutionId, workflowExecutionId);
    if (sweepingOutput == null) {
      return null;
    }
    return sweepingOutput.getVariables();
  }
}
