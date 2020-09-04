package software.wings.yaml.errorhandling;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.GitRepositoryInfo;

@Data
@Builder
public class GitProcessingError {
  private String accountId;
  private String message;
  private Long createdAt;
  private String gitConnectorId;
  private String branchName;
  private String repositoryName;
  private String connectorName;
  private GitRepositoryInfo repositoryInfo;
}
