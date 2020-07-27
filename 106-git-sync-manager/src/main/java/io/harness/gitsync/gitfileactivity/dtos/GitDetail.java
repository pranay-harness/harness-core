package io.harness.gitsync.gitfileactivity.dtos;

import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.EntityType;

@Data
@Builder
public class GitDetail {
  private String entityName;
  private EntityType entityType;
  private String repositoryUrl;
  private String branchName;
  private String yamlGitConfigId;
  private String gitConnectorId;
  private String appId;
  private String gitCommitId;
  @Transient String connectorName;
}
