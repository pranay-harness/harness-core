package io.harness.changestreamsframework;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import com.mongodb.DBObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Builder
@AllArgsConstructor
@Slf4j
@OwnedBy(HarnessTeam.CE)
public class ChangeEvent<T extends PersistentEntity> {
  @NonNull private String token;
  @NonNull private ChangeType changeType;
  @NonNull private Class<T> entityType;
  @NonNull private String uuid;
  private DBObject fullDocument;
  private DBObject changes;

  public boolean isChangeFor(Class<? extends PersistentEntity> entityClass) {
    return this.entityType.isAssignableFrom(entityClass);
  }
}
