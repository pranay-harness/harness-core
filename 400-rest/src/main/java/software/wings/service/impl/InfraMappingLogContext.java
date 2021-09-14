/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class InfraMappingLogContext extends AutoLogContext {
  public static final String ID = "infraMappingId";

  public InfraMappingLogContext(String infraMappingId, OverrideBehavior behavior) {
    super(ID, infraMappingId, behavior);
  }
}
