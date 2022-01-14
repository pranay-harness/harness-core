package software.wings.sm.states.gcbconfigs;

import lombok.Data;

@Data
public class GcbRemoteBuildSpec {
  public enum RemoteFileSource { BRANCH, COMMIT }

  private String gitConfigId;
  private String sourceId;
  private String filePath;
  private String repoName;
  private RemoteFileSource fileSource;
}
