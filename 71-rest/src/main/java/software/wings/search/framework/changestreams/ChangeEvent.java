package software.wings.search.framework.changestreams;

import com.mongodb.DBObject;
import io.harness.persistence.PersistentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * The changeTask bean in which all the changes
 * received from ChangeTrackingTask are converted to.
 *
 * @author utkarsh
 */

@Value
@Builder
@AllArgsConstructor
@Slf4j
public class ChangeEvent<T extends PersistentEntity> {
  @NonNull private String token;
  @NonNull private ChangeType changeType;
  @NonNull private Class<T> entityType;
  @NonNull private String uuid;
  private T fullDocument;
  private DBObject changes;

  public boolean isChangeFor(Class<? extends PersistentEntity> entityClass) {
    return this.entityType.isAssignableFrom(entityClass);
  }
}