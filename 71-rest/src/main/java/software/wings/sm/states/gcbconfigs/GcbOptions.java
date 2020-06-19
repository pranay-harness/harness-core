package software.wings.sm.states.gcbconfigs;

import lombok.Getter;
import lombok.Setter;

public class GcbOptions {
  @Getter @Setter private String gcpConfigId;
  @Getter @Setter private String projectId;
  @Getter @Setter private GcbSpecSource specSource;
  @Getter @Setter private String inlineSpec;
  @Getter @Setter private GcbTriggerBuildSpec triggerSpec;
  @Getter @Setter private GcbRemoteBuildSpec repositorySpec;

  public enum GcbSpecSource { INLINE, REMOTE, TRIGGER }
}
