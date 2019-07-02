package software.wings.beans.trigger;

import lombok.Builder;
import lombok.Value;
import software.wings.beans.Variable;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class TriggerArgs {
  private List<ArtifactSelection> artifactSelections = new ArrayList<>();
  private boolean excludeHostsWithSameArtifact;
  private List<Variable> variables;
}
