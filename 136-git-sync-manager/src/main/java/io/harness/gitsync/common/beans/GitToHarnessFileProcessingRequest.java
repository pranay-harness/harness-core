package io.harness.gitsync.common.beans;

import io.harness.git.model.ChangeType;
import io.harness.gitsync.common.dtos.GitFileChangeDTO;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitToHarnessFileProcessingRequest {
  GitFileChangeDTO fileDetails;
  ChangeType changeType;
}
