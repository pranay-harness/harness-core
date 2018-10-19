package software.wings.expression;

import static java.lang.String.format;

import io.harness.expression.LateBindingMap;
import lombok.Builder;
import software.wings.beans.SweepingOutput;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.utils.KryoUtils;

@Builder
public class SweepingOutputFunctor extends LateBindingMap {
  private String appId;
  private String pipelineExecutionId;
  private String workflowExecutionId;
  private String phaseExecutionId;

  private SweepingOutputService sweepingOutputService;

  public Object output(String name) {
    SweepingOutput sweepingOutput =
        sweepingOutputService.find(appId, name, pipelineExecutionId, workflowExecutionId, phaseExecutionId);
    if (sweepingOutput == null) {
      throw new RuntimeException(format("Missing sweeping output %s", name));
    }
    return KryoUtils.asInflatedObject(sweepingOutput.getOutput());
  }

  @Override
  public Object get(Object key) {
    return output((String) key);
  }
}
