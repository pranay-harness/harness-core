package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Transient;

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
  @Transient String connectorName;
}
