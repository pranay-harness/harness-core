package software.wings.beans;

import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffRequest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitOperationContext {
  private String gitConnectorId;
  private GitConfig gitConfig;

  private GitCommitRequest gitCommitRequest;
  private GitDiffRequest gitDiffRequest;
}
