package software.wings.expression;

import io.harness.beans.SweepingOutput;
import io.harness.expression.LateBindingValue;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import software.wings.service.intfc.SweepingOutputService;

@Builder
public class SweepingOutputValue implements LateBindingValue {
  private String name;
  private String appId;
  private String pipelineExecutionId;
  private String workflowExecutionId;
  private String phaseExecutionId;
  private String stateExecutionId;

  private SweepingOutputService sweepingOutputService;

  @Override
  public Object bind() {
    SweepingOutput sweepingOutput = sweepingOutputService.find(
        appId, name, pipelineExecutionId, workflowExecutionId, phaseExecutionId, stateExecutionId);
    if (sweepingOutput == null) {
      return null;
    }
    return KryoUtils.asInflatedObject(sweepingOutput.getOutput());
  }
}
