package io.harness.gitsync.scm;
import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.ng.core.utils.NGYamlUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class EntityToYamlStringUtils {
  public static String getYamlString(
      GitSyncableEntity entity, GitSdkEntityHandlerInterface gitPersistenceHelperService) {
    if (entity == null) {
      return null;
    }
    final Supplier<YamlDTO> yamlFromEntitySupplier = gitPersistenceHelperService.getYamlFromEntity(entity);
    final YamlDTO yamlObject = yamlFromEntitySupplier.get();
    return NGYamlUtils.getYamlString(yamlObject);
  }

  public static String getYamlString(
      GitSyncableEntity entity, GitSdkEntityHandlerInterface gitPersistenceHelperService, ObjectMapper objectMapper) {
    if (entity == null) {
      return null;
    }
    final Supplier<YamlDTO> yamlFromEntitySupplier = gitPersistenceHelperService.getYamlFromEntity(entity);
    final YamlDTO yamlObject = yamlFromEntitySupplier.get();
    return NGYamlUtils.getYamlString(yamlObject);
  }
}
