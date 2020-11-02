package software.wings.service.impl;

import static io.harness.persistence.HQuery.excludeAuthority;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.EntityVersionCollection.Builder.anEntityVersionCollection;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.EntityVersion.EntityVersionKeys;
import software.wings.beans.EntityVersionCollection;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EntityVersionService;

/**
 * Created by rishi on 10/18/16.
 */
@Singleton
@Slf4j
public class EntityVersionServiceImpl implements EntityVersionService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;

  @Override
  public PageResponse<EntityVersionCollection> listEntityVersions(PageRequest<EntityVersionCollection> pageRequest) {
    return wingsPersistence.query(EntityVersionCollection.class, pageRequest, excludeAuthority);
  }

  @Override
  public EntityVersion lastEntityVersion(String appId, EntityType entityType, String entityUuid) {
    return lastEntityVersion(appId, entityType, entityUuid, null);
  }

  @Override
  public EntityVersion lastEntityVersion(String appId, EntityType entityType, String entityUuid, String parentUuid) {
    final Query<EntityVersionCollection> query = wingsPersistence.createQuery(EntityVersionCollection.class)
                                                     .filter(EntityVersionCollection.APP_ID_KEY, appId)
                                                     .filter(EntityVersionKeys.entityType, entityType)
                                                     .filter(EntityVersionKeys.entityUuid, entityUuid)
                                                     .order(Sort.descending(EntityVersionKeys.version));

    if (isNotBlank(parentUuid)) {
      query.filter(EntityVersionKeys.entityParentUuid, parentUuid);
    }
    return query.get();
  }

  @Override
  public EntityVersion newEntityVersion(
      String appId, EntityType entityType, String entityUuid, String name, ChangeType changeType) {
    return newEntityVersion(appId, entityType, entityUuid, null, name, changeType, null);
  }

  @Override
  public EntityVersion newEntityVersion(String appId, EntityType entityType, String entityUuid, String parentUuid,
      String name, ChangeType changeType, String entityData) {
    String accountId = appService.getAccountIdByAppId(appId);
    EntityVersionCollection entityVersion = anEntityVersionCollection()
                                                .withAppId(appId)
                                                .withEntityType(entityType)
                                                .withEntityUuid(entityUuid)
                                                .withEntityData(entityData)
                                                .withEntityName(name)
                                                .withChangeType(changeType)
                                                .withEntityParentUuid(parentUuid)
                                                .withAccountId(accountId)
                                                .build();
    int i = 0;
    boolean done = false;
    do {
      try {
        EntityVersion lastEntityVersion = lastEntityVersion(appId, entityType, entityUuid, parentUuid);
        if (lastEntityVersion == null) {
          entityVersion.setVersion(EntityVersion.INITIAL_VERSION);
        } else {
          entityVersion.setVersion(lastEntityVersion.getVersion() + 1);
        }
        wingsPersistence.save(entityVersion);
        done = true;
      } catch (Exception e) {
        log.warn("EntityVersion save failed for entityType: {}, entityUuid: {} - attemptNo: {}", entityType, entityUuid,
            i, e);
        i++;
        // If we exception out then done is still 'false' and we will retry again
        entityVersion.setCreatedAt(0);
      }
    } while (!done && i < 3);

    return entityVersion;
  }

  @Override
  public EntityVersion newEntityVersion(
      String appId, EntityType entityType, String entityUuid, String name, ChangeType changeType, String entityData) {
    return newEntityVersion(appId, entityType, entityUuid, null, name, changeType, entityData);
  }

  @Override
  public void updateEntityData(String appId, String entityVersionUuid, String entityData) {
    Query<EntityVersionCollection> query = wingsPersistence.createQuery(EntityVersionCollection.class)
                                               .filter("appId", appId)
                                               .filter(ID_KEY, entityVersionUuid);
    UpdateOperations<EntityVersionCollection> updateOps =
        wingsPersistence.createUpdateOperations(EntityVersionCollection.class);
    updateOps.set("entityData", entityData);
    UpdateResults updated = wingsPersistence.update(query, updateOps);
    if (updated == null || updated.getUpdatedCount() != 1) {
      log.error("updateEntityData failed for appId: {}, entityVersionUuid: {}- entityData: {}", appId,
          entityVersionUuid, entityData);
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE);
    }
  }
}
