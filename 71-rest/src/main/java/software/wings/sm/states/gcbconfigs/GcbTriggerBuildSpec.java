package software.wings.sm.states.gcbconfigs;

import lombok.Getter;
import lombok.Setter;
import software.wings.sm.states.ParameterEntry;

import java.util.List;

public class GcbTriggerBuildSpec {
  @Getter @Setter private String name;
  @Getter @Setter private String sourceId;
  @Getter @Setter private GcbTriggerSource source;
  @Getter @Setter private List<ParameterEntry> substitutions;

  public enum GcbTriggerSource { TAG, BRANCH, COMMIT }
}
