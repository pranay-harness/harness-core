package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffRequest;

@Data
@Builder
public class GitOperationContext {
  private String gitConnectorId;
  private GitConfig gitConfig;

  private GitCommitRequest gitCommitRequest;
  private GitDiffRequest gitDiffRequest;
}
