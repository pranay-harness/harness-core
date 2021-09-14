/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.LinkedHashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class ParsedQuery extends LinkedHashMap<String, Object> {
  public ParsedQuery() {}

  public ParsedQuery(Map<String, Object> objectMap) {
    super(objectMap);
  }
}
