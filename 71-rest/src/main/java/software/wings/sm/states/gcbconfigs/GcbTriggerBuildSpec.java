package software.wings.sm.states.gcbconfigs;

import lombok.Data;
import software.wings.sm.states.ParameterEntry;

import java.util.List;

@Data
public class GcbTriggerBuildSpec {
  public enum GcbTriggerSource { TAG, BRANCH, COMMIT }

  private String name;
  private String sourceId;
  private GcbTriggerSource source;
  private List<ParameterEntry> substitutions;
}
