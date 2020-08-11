package io.harness.git.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GitBaseRequest {
  private String repoUrl;
  private String branch;
  private String commitId;

  private AuthRequest authRequest;

  private String connectorId;
  private String accountId;
  private String repoType;
}
