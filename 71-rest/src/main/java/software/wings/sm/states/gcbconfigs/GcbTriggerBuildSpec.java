package software.wings.sm.states.gcbconfigs;

import lombok.Data;
import software.wings.beans.NameValuePair;

import java.util.List;

@Data
public class GcbTriggerBuildSpec {
  public enum GcbTriggerSource { TAG, BRANCH, COMMIT }

  private String name;
  private String sourceId;
  private GcbTriggerSource source;
  private List<NameValuePair> substitutions;
}
