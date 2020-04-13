package software.wings.yaml.errorhandling;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "HarnessToGitErrorDetailsKeys")
public class HarnessToGitErrorDetails implements GitSyncErrorDetails {
  private boolean fullSyncPath;
}
