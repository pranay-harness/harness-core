package io.harness.git.model;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString(exclude = {"authRequest"})
public class GitBaseRequest {
  private String repoUrl;
  private String branch;
  private String commitId;

  private AuthRequest authRequest;

  private String connectorId;
  private String accountId;
  private String repoType;

  public boolean useBranch() {
    return isNotEmpty(branch);
  }

  public boolean useCommitId() {
    return isNotEmpty(commitId);
  }
}
