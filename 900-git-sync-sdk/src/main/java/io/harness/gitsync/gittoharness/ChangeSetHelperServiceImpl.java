package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.GitSyncEntitiesConfiguration;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.exceptions.NGYamlParsingException;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.ng.core.event.EventProtoToEntityHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class ChangeSetHelperServiceImpl implements GitSdkInterface {
  @Inject
  @Named("GitSyncEntityConfigurations")
  Map<EntityType, GitSyncEntitiesConfiguration> gitSyncEntityConfigurationsMap;
  @Inject @Named("GitSyncObjectMapper") ObjectMapper objectMapper;
  @Inject Map<String, GitSdkEntityHandlerInterface> gitPersistenceHelperServiceMap;

  @Override
  public void process(ChangeSet changeSet) {
    EntityType entityType = EventProtoToEntityHelper.getEntityTypeFromProto(changeSet.getEntityType());
    GitSyncEntitiesConfiguration gitSyncEntitiesConfiguration = gitSyncEntityConfigurationsMap.get(entityType);
    Class<? extends GitSyncableEntity> entityClass = gitSyncEntitiesConfiguration.getEntityClass();
    GitSdkEntityHandlerInterface entityGitPersistenceHelperService =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName());
    String yaml = changeSet.getYaml();
    switch (changeSet.getChangeType()) {
      case ADD:
        Class<? extends YamlDTO> yamlClass = gitSyncEntitiesConfiguration.getYamlClass();
        YamlDTO yamlDTO = convertStringToDTO(yaml, yamlClass);
        entityGitPersistenceHelperService.save(yamlDTO, changeSet.getAccountId());
        break;
      case DELETE:
        // todo @deepak : add the function to get the entity reference for this connector
        entityGitPersistenceHelperService.delete(null);
        break;
      case MODIFY:
        Class<? extends YamlDTO> yamlClassForUpdate = gitSyncEntitiesConfiguration.getYamlClass();
        YamlDTO updatedYamlDTO = convertStringToDTO(yaml, yamlClassForUpdate);
        entityGitPersistenceHelperService.update(updatedYamlDTO, changeSet.getAccountId());
        break;
      case UNRECOGNIZED:
      default:
        throw new UnexpectedException(String.format("Got unrecognized change set type for changeset [{}]", changeSet));
    }
  }

  private YamlDTO convertStringToDTO(String yaml, Class<? extends YamlDTO> yamlClass) {
    try {
      return objectMapper.readValue(yaml, yamlClass);
    } catch (IOException ex) {
      log.error("Error converting the yaml file [%s]", yaml, ex);
      throw new NGYamlParsingException(String.format("Could not parse the YAML %s", yaml));
    }
  }
}