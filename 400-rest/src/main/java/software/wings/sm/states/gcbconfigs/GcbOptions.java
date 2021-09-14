/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
