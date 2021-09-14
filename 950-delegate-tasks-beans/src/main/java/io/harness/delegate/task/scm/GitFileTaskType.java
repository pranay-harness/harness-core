/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
public enum GitFileTaskType {
  GET_FILE_CONTENT_BATCH,
  GET_FILE_CONTENT,
  GET_FILE_CONTENT_BATCH_BY_FILE_PATHS,
  GET_FILE_CONTENT_BATCH_BY_REF
}
