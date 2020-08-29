package io.harness.gitsync.gitsyncerror.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitProcessingError {
  private String accountId;
  private String message;
  private Long createdAt;
  private String gitConnectorId;
  private String branchName;
  private String connectorName;
}
