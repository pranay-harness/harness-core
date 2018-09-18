package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.EntityVersionCollection;

/**
 * Created by rishi on 10/18/16.
 */
public interface EntityVersionService {
  PageResponse<EntityVersionCollection> listEntityVersions(PageRequest<EntityVersionCollection> pageRequest);

  EntityVersion lastEntityVersion(String appId, EntityType entityType, String entityUuid);

  EntityVersion lastEntityVersion(String appId, EntityType entityType, String entityUuid, String parentUuid);

  EntityVersion newEntityVersion(
      String appId, EntityType entityType, String entityUuid, String name, ChangeType changeType);

  EntityVersion newEntityVersion(
      String appId, EntityType entityType, String entityUuid, String name, ChangeType changeType, String entityData);

  EntityVersion newEntityVersion(String appId, EntityType entityType, String entityUuid, String parentUuid, String name,
      ChangeType changeType, String entityData);

  void updateEntityData(String appId, String entityVersionUuid, String entityData);
}
