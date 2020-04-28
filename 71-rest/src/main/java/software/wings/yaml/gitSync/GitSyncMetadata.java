package software.wings.yaml.gitSync;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitSyncMetadata {
  String gitConnectorId;
  String branchName;
  String yamlGitConfigId;
}
