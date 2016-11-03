package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.EntityVersionCollection.Builder.anEntityVersionCollection;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersionCollection;
import software.wings.beans.ErrorCodes;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.EntityVersionService;

import javax.inject.Inject;

/**
 * Created by rishi on 10/18/16.
 */
public class EntityVersionServiceImpl implements EntityVersionService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<EntityVersionCollection> listEntityVersions(PageRequest<EntityVersionCollection> pageRequest) {
    return wingsPersistence.query(EntityVersionCollection.class, pageRequest);
  }

  @Override
  public EntityVersion lastEntityVersion(String appId, EntityType entityType, String entityUuid) {
    PageRequest<EntityVersionCollection> pageRequest = aPageRequest()
                                                           .addFilter("appId", Operator.EQ, appId)
                                                           .addFilter("entityType", Operator.EQ, entityType)
                                                           .addFilter("entityUuid", Operator.EQ, entityUuid)
                                                           .build();
    return wingsPersistence.get(EntityVersionCollection.class, pageRequest);
  }

  @Override
  public EntityVersion newEntityVersion(String appId, EntityType entityType, String entityUuid) {
    return newEntityVersion(appId, entityType, entityUuid, null);
  }

  @Override
  public EntityVersion newEntityVersion(String appId, EntityType entityType, String entityUuid, String entityData) {
    EntityVersionCollection entityVersion = null;
    int i = 0;
    boolean done = false;
    do {
      try {
        entityVersion = anEntityVersionCollection()
                            .withAppId(appId)
                            .withEntityType(entityType)
                            .withEntityUuid(entityUuid)
                            .withEntityData(entityData)
                            .build();
        EntityVersion lastEntityVersion = lastEntityVersion(appId, entityType, entityUuid);
        if (lastEntityVersion == null) {
          entityVersion.setVersion(EntityVersion.INITIAL_VERSION);
        } else {
          entityVersion.setVersion(lastEntityVersion.getVersion() + 1);
        }
        entityVersion = wingsPersistence.saveAndGet(EntityVersionCollection.class, entityVersion);
        done = true;
      } catch (Exception e) {
        logger.warn(
            "EntityVersion save failed for entityType: {}, entityUuid: {}- attemptNo: {}", entityType, entityUuid, i);
        i++;
      }
    } while (!done && i < 3);

    return entityVersion;
  }

  @Override
  public void updateEntityData(String appId, String entityVersionUuid, String entityData) {
    Query<EntityVersionCollection> query = wingsPersistence.createQuery(EntityVersionCollection.class)
                                               .field("appId")
                                               .equal(appId)
                                               .field(ID_KEY)
                                               .equal(entityVersionUuid);
    UpdateOperations<EntityVersionCollection> updateOps =
        wingsPersistence.createUpdateOperations(EntityVersionCollection.class);
    updateOps.set("entityData", entityData);
    UpdateResults updated = wingsPersistence.update(query, updateOps);
    if (updated == null || updated.getUpdatedCount() != 1) {
      logger.error("updateEntityData failed for appId: {}, entityVersionUuid: {}- entityData: {}", appId,
          entityVersionUuid, entityData);
      throw new WingsException(ErrorCodes.DEFAULT_ERROR_CODE);
    }
  }
}
