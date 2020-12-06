package software.wings.sm.states.gcbconfigs;

import lombok.Data;

@Data
public class GcbOptions {
  public enum GcbSpecSource { INLINE, REMOTE, TRIGGER }

  private String gcpConfigId;
  private GcbSpecSource specSource;
  private String inlineSpec;
  private GcbTriggerBuildSpec triggerSpec;
  private GcbRemoteBuildSpec repositorySpec;
}
