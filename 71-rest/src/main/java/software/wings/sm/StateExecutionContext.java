package software.wings.sm;

import io.harness.delegate.task.shell.ScriptType;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.artifact.Artifact;

import java.util.List;

@Value
@Builder
public class StateExecutionContext {
  private StateExecutionData stateExecutionData;
  private Artifact artifact;
  private boolean adoptDelegateDecryption;
  private int expressionFunctorToken;
  List<ContextElement> contextElements;
  private ScriptType scriptType;
}
