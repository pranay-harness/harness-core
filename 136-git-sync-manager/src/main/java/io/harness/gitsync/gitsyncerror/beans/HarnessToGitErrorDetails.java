package io.harness.gitsync.gitsyncerror.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@FieldNameConstants(innerTypeName = "HarnessToGitErrorDetailsKeys")
@TypeAlias("io.harness.gitsync.gitsyncerror.beans.harnessToGitErrorDetails")
@OwnedBy(PL)
public class HarnessToGitErrorDetails implements GitSyncErrorDetails {
  @Trimmed private String orgIdentifier;
  @Trimmed private String projectIdentifier;
}
