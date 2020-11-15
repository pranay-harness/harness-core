package io.harness.git.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class FetchFilesBwCommitsRequest extends GitBaseRequest {
  private String newCommitId;
  private String oldCommitId;
}
