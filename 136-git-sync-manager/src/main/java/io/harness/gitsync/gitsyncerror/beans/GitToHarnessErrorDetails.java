package io.harness.gitsync.gitsyncerror.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@FieldNameConstants(innerTypeName = "GitToHarnessErrorDetailsKeys")
@TypeAlias("io.harness.gitsync.gitsyncerror.beans.gitToHarnessErrorDetails")
@OwnedBy(PL)
public class GitToHarnessErrorDetails implements GitSyncErrorDetails {
  @NotEmpty private String gitCommitId;
  private String yamlContent;
  private String commitMessage;
  private String resolvedByCommitId;
}
