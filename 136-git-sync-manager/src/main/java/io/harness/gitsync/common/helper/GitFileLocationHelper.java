package io.harness.gitsync.common.helper;

import static io.harness.gitsync.common.YamlConstants.EXTENSION_SEPARATOR;
import static io.harness.gitsync.common.YamlConstants.PATH_DELIMITER;
import static io.harness.gitsync.common.YamlConstants.YAML_EXTENSION;

import io.harness.EntityType;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class GitFileLocationHelper {
  public static String getEntityPath(String rootPath, String entityType, String entityId) {
    return rootPath + PATH_DELIMITER + entityType + PATH_DELIMITER + entityId + EXTENSION_SEPARATOR + YAML_EXTENSION;
  }

  public static String getRootPathSafely(String filePath) {
    try {
      final String pathWithoutIdentifier = filePath.substring(0, filePath.lastIndexOf(PATH_DELIMITER));
      return pathWithoutIdentifier.substring(0, pathWithoutIdentifier.lastIndexOf(PATH_DELIMITER));
    } catch (Exception e) {
      log.error("Not able to find root path for {}", filePath);
      return "";
    }
  }

  public static EntityType getEntityType(String filePath) {
    final String pathWithoutIdentifier = filePath.substring(0, filePath.lastIndexOf(PATH_DELIMITER));
    final String stringType = pathWithoutIdentifier.substring(pathWithoutIdentifier.lastIndexOf(PATH_DELIMITER) + 1);
    return EntityType.getEntityFromYamlType(stringType);
  }
}
