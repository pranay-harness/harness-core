/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.expression;

import io.harness.beans.SweepingOutput;

import java.util.HashMap;

public class MapTestSweepingOutput extends HashMap<String, Object> implements SweepingOutput {
  @Override
  public String getType() {
    return "mapTestSweepingOutput";
  }
}
