package software.wings.service.intfc;

import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersionCollection;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * Created by rishi on 10/18/16.
 */
public interface EntityVersionService {
  PageResponse<EntityVersionCollection> listEntityVersions(PageRequest<EntityVersionCollection> pageRequest);

  EntityVersion lastEntityVersion(String appId, EntityType entityType, String entityUuid);

  EntityVersion newEntityVersion(String appId, EntityType entityType, String entityUuid);

  EntityVersion newEntityVersion(String appId, EntityType entityType, String entityUuid, String entityData);

  void updateEntityData(String appId, String entityVersionUuid, String entityData);
}
