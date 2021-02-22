package io.harness.gitsync.gitsyncerror.beans;

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
public class HarnessToGitErrorDetails implements GitSyncErrorDetails {
  private boolean fullSyncPath;
}
