/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.script;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScriptInfo {
  // TODO Improve it by taking proper script type and input format
  private String scriptString;

  private boolean fromTemplate;
}
