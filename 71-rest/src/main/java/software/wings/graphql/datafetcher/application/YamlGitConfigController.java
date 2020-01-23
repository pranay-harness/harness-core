package software.wings.graphql.datafetcher.application;

import lombok.experimental.UtilityClass;
import software.wings.graphql.schema.type.QLGitSyncConfig.QLGitSyncConfigBuilder;
import software.wings.yaml.gitSync.YamlGitConfig;

@UtilityClass
public class YamlGitConfigController {
  public static QLGitSyncConfigBuilder populateQLGitConfig(
      YamlGitConfig yamlGitConfig, QLGitSyncConfigBuilder builder) {
    return builder.gitConnectorId(yamlGitConfig.getGitConnectorId())
        .branch(yamlGitConfig.getBranchName())
        .syncEnabled(yamlGitConfig.isEnabled());
  }
}
