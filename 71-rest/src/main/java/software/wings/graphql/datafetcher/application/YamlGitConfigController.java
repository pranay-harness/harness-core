package software.wings.graphql.datafetcher.application;

import lombok.experimental.UtilityClass;
import software.wings.graphql.schema.type.QLGitConfig.QLGitConfigBuilder;
import software.wings.yaml.gitSync.YamlGitConfig;

@UtilityClass
public class YamlGitConfigController {
  public static QLGitConfigBuilder populateQLGitConfig(YamlGitConfig yamlGitConfig, QLGitConfigBuilder builder) {
    return builder.gitConnectorId(yamlGitConfig.getGitConnectorId())
        .branch(yamlGitConfig.getBranchName())
        .syncEnabled(yamlGitConfig.isEnabled());
  }
}
