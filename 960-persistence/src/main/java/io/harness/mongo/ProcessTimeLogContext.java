/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo;

import io.harness.logging.AutoLogContext;

public class ProcessTimeLogContext extends AutoLogContext {
  public static final String ID = "processTime";

  public ProcessTimeLogContext(Long processTime, OverrideBehavior behavior) {
    super(ID, processTime.toString(), behavior);
  }
}
