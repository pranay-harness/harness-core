/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.merger.fqn;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FQNNode {
  public enum NodeType { KEY, KEY_WITH_UUID, PARALLEL, UUID }

  private NodeType nodeType;
  private String key;
  private String uuidKey;
  private String uuidValue;
}
