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
@Entity(value = "searchSourceEntitiesSyncState", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "SearchSourceEntitySyncStateKeys")
@Slf4j
public class SearchSourceEntitySyncState implements PersistentEntity {
  @Id private String sourceEntityClass;
  private String lastSyncedToken;
}
