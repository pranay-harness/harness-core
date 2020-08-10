package io.harness.gitsync.common;

import static io.harness.gitsync.common.YamlConstants.EXTENSION_SEPARATOR;
import static io.harness.gitsync.common.YamlConstants.PATH_DELIMITER;
import static io.harness.gitsync.common.YamlConstants.YAML_EXTENSION;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GitFileLocationHelper {
  public static String getEntityPath(String rootPath, String entityType, String entityId) {
    return rootPath + PATH_DELIMITER + entityType + PATH_DELIMITER + entityId + EXTENSION_SEPARATOR + YAML_EXTENSION;
  }
}
