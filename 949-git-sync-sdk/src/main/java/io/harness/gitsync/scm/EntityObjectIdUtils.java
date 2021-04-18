package io.harness.gitsync.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.common.helper.GitObjectIdHelper;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.ng.core.utils.NGYamlUtils;

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class EntityObjectIdUtils {
  private static YAMLMapper yamlMapper = new YAMLMapper().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);

  public static String getObjectIdOfYaml(YamlDTO yamlObject) {
    final String yamlString = NGYamlUtils.getYamlString(yamlObject);
    return GitObjectIdHelper.getObjectIdForString(yamlString);
  }

  public static String getObjectIdOfYaml(
      GitSyncableEntity entity, GitSdkEntityHandlerInterface gitPersistenceHelperService) {
    final String yamlString = EntityToYamlStringUtils.getYamlString(entity, gitPersistenceHelperService);
    return GitObjectIdHelper.getObjectIdForString(yamlString);
  }

  public static String getObjectIdOfYaml(String yamlString) {
    return GitObjectIdHelper.getObjectIdForString(yamlString);
  }
}
