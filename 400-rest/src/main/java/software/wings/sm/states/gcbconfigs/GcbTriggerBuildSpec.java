/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states.gcbconfigs;

import software.wings.beans.NameValuePair;

import java.util.List;
import lombok.Data;

@Data
public class GcbTriggerBuildSpec {
  public enum GcbTriggerSource { TAG, BRANCH, COMMIT }

  private String name;
  private String sourceId;
  private GcbTriggerSource source;
  private List<NameValuePair> substitutions;
}
