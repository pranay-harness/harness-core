package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
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

@OwnedBy(CDC)
@Value
@Entity(value = "timeScaleSourceEntitiesSyncState", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "TimeScaleSourceEntitySyncStateKeys")
@Slf4j
public class TimeScaleSourceEntitySyncState implements PersistentEntity {
  @Id private String timeScaleEntityClass;
  private String lastSyncedToken;
}
