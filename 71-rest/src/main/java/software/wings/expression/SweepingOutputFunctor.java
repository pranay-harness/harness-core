package software.wings.expression;

import static java.lang.String.format;

import io.harness.beans.SweepingOutput;
import io.harness.expression.LateBindingMap;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import software.wings.exception.SweepingOutputException;
import software.wings.service.intfc.SweepingOutputService;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
public class SweepingOutputFunctor extends LateBindingMap {
  private String appId;
  private String pipelineExecutionId;
  private String workflowExecutionId;
  private String phaseExecutionId;
  private String stateExecutionId;

  private transient SweepingOutputService sweepingOutputService;

  public Object output(String name) {
    SweepingOutput sweepingOutput = sweepingOutputService.find(
        appId, name, pipelineExecutionId, workflowExecutionId, phaseExecutionId, stateExecutionId);
    if (sweepingOutput == null) {
      throw new SweepingOutputException(format("Missing sweeping output %s", name));
    }
    return KryoUtils.asInflatedObject(sweepingOutput.getOutput());
  }

  @Override
  public Object get(Object key) {
    return output((String) key);
  }
}
