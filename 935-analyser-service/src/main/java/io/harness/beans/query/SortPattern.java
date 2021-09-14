/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.query;

import java.util.LinkedHashMap;
import java.util.Map;

public class SortPattern extends LinkedHashMap<String, Object> {
  public SortPattern() {}

  public SortPattern(Map<String, Object> objectMap) {
    super(objectMap);
  }
}
