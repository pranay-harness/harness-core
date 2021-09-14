/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.application;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLGitSyncConfig.QLGitSyncConfigBuilder;
import software.wings.yaml.gitSync.YamlGitConfig;

import lombok.experimental.UtilityClass;

@UtilityClass
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class YamlGitConfigController {
  public static QLGitSyncConfigBuilder populateQLGitConfig(
      YamlGitConfig yamlGitConfig, QLGitSyncConfigBuilder builder) {
    return builder.gitConnectorId(yamlGitConfig.getGitConnectorId())
        .branch(yamlGitConfig.getBranchName())
        .repositoryName(yamlGitConfig.getRepositoryName())
        .syncEnabled(yamlGitConfig.isEnabled());
  }
}
