package software.wings.beans.trigger;

import lombok.Builder;
import lombok.Value;
import software.wings.beans.Variable;

import java.util.List;

@Value
@Builder
public class TriggerArgs {
  private List<TriggerArtifactVariable> triggerArtifactVariables;
  private boolean excludeHostsWithSameArtifact;
  private List<Variable> variables;
}
