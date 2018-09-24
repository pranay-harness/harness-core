package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.EntityVersionCollection.Builder.anEntityVersionCollection;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.persistence.HQuery;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.EntityVersionCollection;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.EntityVersionService;

/**
 * Created by rishi on 10/18/16.
 */
@Singleton
public class EntityVersionServiceImpl implements EntityVersionService {
  private static final Logger logger = LoggerFactory.getLogger(EntityVersionServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<EntityVersionCollection> listEntityVersions(PageRequest<EntityVersionCollection> pageRequest) {
    return wingsPersistence.query(EntityVersionCollection.class, pageRequest, HQuery.excludeAuthority);
  }

  @Override
  public EntityVersion lastEntityVersion(String appId, EntityType entityType, String entityUuid) {
    return lastEntityVersion(appId, entityType, entityUuid, null);
  }

  @Override
  public EntityVersion lastEntityVersion(String appId, EntityType entityType, String entityUuid, String parentUuid) {
    PageRequest<EntityVersionCollection> pageRequest = aPageRequest()
                                                           .addFilter("appId", Operator.EQ, appId)
                                                           .addFilter("entityType", Operator.EQ, entityType)
                                                           .addFilter("entityUuid", Operator.EQ, entityUuid)
                                                           .build();
    if (isNotBlank(parentUuid)) {
      pageRequest.addFilter("entityParentUuid", Operator.EQ, parentUuid);
    }
    return wingsPersistence.get(EntityVersionCollection.class, pageRequest);
  }

  @Override
  public EntityVersion newEntityVersion(
      String appId, EntityType entityType, String entityUuid, String name, ChangeType changeType) {
    return newEntityVersion(appId, entityType, entityUuid, null, name, changeType, null);
  }

  @Override
  public EntityVersion newEntityVersion(String appId, EntityType entityType, String entityUuid, String parentUuid,
      String name, ChangeType changeType, String entityData) {
    EntityVersionCollection entityVersion = anEntityVersionCollection()
                                                .withAppId(appId)
                                                .withEntityType(entityType)
                                                .withEntityUuid(entityUuid)
                                                .withEntityData(entityData)
                                                .withEntityName(name)
                                                .withChangeType(changeType)
                                                .withEntityParentUuid(parentUuid)
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
        entityVersion = wingsPersistence.saveAndGet(EntityVersionCollection.class, entityVersion);
        done = true;
      } catch (Exception e) {
        logger.warn(format("EntityVersion save failed for entityType: %s, entityUuid: %s- attemptNo: %s", entityType,
                        entityUuid, i),
            e);
        i++;
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
      logger.error("updateEntityData failed for appId: {}, entityVersionUuid: {}- entityData: {}", appId,
          entityVersionUuid, entityData);
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE);
    }
  }
}
