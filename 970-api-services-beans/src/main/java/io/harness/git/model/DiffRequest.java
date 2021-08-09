package io.harness.git.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DiffRequest extends GitBaseRequest {
  private String lastProcessedCommitId;
  private String endCommitId;
  private boolean excludeFilesOutsideSetupFolder;
}
