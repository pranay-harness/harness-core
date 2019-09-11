package software.wings.search.framework;

import io.harness.persistence.PersistentEntity;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * The current sync state representation
 * of a search entity.
 *
 * @author utkarsh
 */

@Value
@Entity(value = "searchEntitiesSyncState", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "SearchEntitySyncStateKeys")
@Slf4j
public class SearchEntitySyncState implements PersistentEntity {
  @Id private String searchEntityClass;
  private String syncVersion;
  private String lastSyncedToken;

  private SearchEntity getSearchEntity() {
    try {
      return (SearchEntity) Class.forName(searchEntityClass).newInstance();
    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
      logger.error("Could not create new instance", e);
    }
    return null;
  }

  public boolean shouldBulkSync() {
    return !getSearchEntity().getVersion().equals(syncVersion);
  }
}
