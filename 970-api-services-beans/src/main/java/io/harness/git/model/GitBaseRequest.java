package io.harness.git.model;

import static io.harness.data.structure.HasPredicate.hasSome;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@SuperBuilder
@ToString(exclude = {"authRequest"})
public class GitBaseRequest {
  private String repoUrl;
  private String branch;
  private String commitId;

  private AuthRequest authRequest;

  private String connectorId;
  @NotEmpty private String accountId;
  private GitRepositoryType repoType;

  public boolean useBranch() {
    return hasSome(branch);
  }

  public boolean useCommitId() {
    return hasSome(commitId);
  }
}
