package io.harness.gitsync.gitsyncerror.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "HarnessToGitErrorDetailsKeys")
@Document("harnessToGitErrorDetailsNG")
@TypeAlias("io.harness.gitsync.gitsyncerror.beans.harnessToGitErrorDetails")
@OwnedBy(DX)
public class HarnessToGitErrorDetails implements GitSyncErrorDetails {
  private boolean fullSyncPath;
}
