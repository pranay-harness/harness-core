/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
