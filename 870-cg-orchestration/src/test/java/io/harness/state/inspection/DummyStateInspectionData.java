/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.state.inspection;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DummyStateInspectionData implements StateInspectionData {
  private String value;

  @Override
  public String key() {
    return "dummy";
  }
}
