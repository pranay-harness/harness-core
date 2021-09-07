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
