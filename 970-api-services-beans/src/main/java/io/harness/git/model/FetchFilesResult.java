package io.harness.git.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FetchFilesResult extends GitBaseResult {
  private CommitResult commitResult;
  private List<GitFile> files;

  @Builder
  public FetchFilesResult(String accountId, CommitResult commitResult, List<GitFile> files) {
    super(accountId);
    this.commitResult = commitResult;
    this.files = files;
  }
}
