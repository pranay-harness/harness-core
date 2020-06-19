package software.wings.sm.states.gcbconfigs;

import lombok.Getter;
import lombok.Setter;

public class GcbRemoteBuildSpec {
  @Getter @Setter private String sourceId;
  @Getter @Setter private String filePath;
  @Getter @Setter private RemoteFileSource fileSource;

  public enum RemoteFileSource { BRANCH, COMMIT }
}
