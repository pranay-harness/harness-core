package software.wings.graphql.datafetcher.application;

import software.wings.graphql.schema.type.QLGitSyncConfig.QLGitSyncConfigBuilder;
import software.wings.yaml.gitSync.YamlGitConfig;

import lombok.experimental.UtilityClass;

@UtilityClass
public class YamlGitConfigController {
  public static QLGitSyncConfigBuilder populateQLGitConfig(
      YamlGitConfig yamlGitConfig, QLGitSyncConfigBuilder builder) {
    return builder.gitConnectorId(yamlGitConfig.getGitConnectorId())
        .branch(yamlGitConfig.getBranchName())
        .repositoryName(yamlGitConfig.getRepositoryName())
        .syncEnabled(yamlGitConfig.isEnabled());
  }
}
