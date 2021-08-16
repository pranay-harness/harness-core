package io.harness.gitsync.common;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(DX)
@TargetModule(HarnessModule._870_CG_YAML)
public class YamlConstants {
  public static final String PATH_DELIMITER = "/";
  public static final String YAML_EXTENSION = "yaml";
  public static final String EXTENSION_SEPARATOR = ".";
  public static final String HARNESS_FOLDER_EXTENSION = ".harness";
  public static final String GITSYNC_CONFIG_ID = "gitSyncConfigId";
  public static final String BRANCH = "branch";
  public static final String YAML_GIT_CONFIG = "yamlGitConfigIdentifier";
  public static final String FILE_PATH = "filePath";
  public static final String COMMIT_ID = "commitId";
}
