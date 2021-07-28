package io.harness.gitsync.entityInfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.gitsync.scm.EntityObjectIdUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DX)
public abstract class AbstractGitSdkEntityHandler<B extends GitSyncableEntity, Y extends YamlDTO>
    implements GitSdkEntityHandlerInterface<B, Y> {
  @Override
  public Y upsert(String accountIdentifier, String yaml) {
    final String lastObjectId = getLastObjectIdIfExists(accountIdentifier, yaml);
    if (lastObjectId != null) {
      final String objectIdOfNewYaml = EntityObjectIdUtils.getObjectIdOfYaml(yaml);
      if (lastObjectId.equals(objectIdOfNewYaml)) {
        log.info("Object already processed hence skipping database update.");
        return getYamlDTO(yaml);
      }
      log.info("Object Id differs for database object: [{}] and git object: [{}] hence updating.", lastObjectId,
          objectIdOfNewYaml);
      return update(accountIdentifier, yaml);
    } else {
      log.info("Object not found for yaml hence creating in database");
      return save(accountIdentifier, yaml);
    }
  }

  public abstract String getLastObjectIdIfExists(String accountIdentifier, String yaml);

  public abstract Y getYamlDTO(String yaml);
}
