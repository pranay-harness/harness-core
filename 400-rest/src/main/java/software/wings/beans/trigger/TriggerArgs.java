package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.Variable;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class TriggerArgs {
  private List<TriggerArtifactVariable> triggerArtifactVariables;
  private boolean excludeHostsWithSameArtifact;
  private List<Variable> variables;
}
