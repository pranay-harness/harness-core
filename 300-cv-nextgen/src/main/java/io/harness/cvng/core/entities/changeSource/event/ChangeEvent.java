package io.harness.cvng.core.entities.changeSource.event;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.services.api.UpdatableEntity;
import io.harness.cvng.core.types.ChangeSourceType;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "ChangeEventKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "changeEvents")
@OwnedBy(HarnessTeam.CV)
@HarnessEntity(exportable = true)
@StoreIn(DbAliases.CVNG)
public abstract class ChangeEvent
    implements PersistentEntity, UuidAware, AccountAccess, UpdatedAtAware, CreatedAtAware {
  @Id String uuid;
  long createdAt;
  long lastUpdatedAt;

  @NotNull String accountId;
  @NotNull String name;
  @NotNull String orgIdentifier;
  @NotNull String projectIdentifier;

  @NotNull String serviceIdentifier;
  @NotNull String envIdentifier;

  @NotNull String changeSourceIdentifier;
  @NotNull ChangeSourceType type;

  long eventTime;

  public abstract static class ChangeEventUpdatableEntity<T extends ChangeEvent, D extends ChangeEvent>
      implements UpdatableEntity<T, D> {
    public abstract Class getEntityClass();

    public Query<T> populateKeyQuery(Query<T> query, D changeEvent) {
      return query.filter(ChangeEventKeys.orgIdentifier, changeEvent.getOrgIdentifier())
          .filter(ChangeEventKeys.projectIdentifier, changeEvent.getProjectIdentifier())
          .filter(ChangeEventKeys.serviceIdentifier, changeEvent.getServiceIdentifier())
          .filter(ChangeEventKeys.envIdentifier, changeEvent.getEnvIdentifier());
    }

    protected void setCommonUpdateOperations(UpdateOperations<T> updateOperations, D changeEvent) {
      updateOperations.set(ChangeEventKeys.accountId, changeEvent.getAccountId())
          .set(ChangeEventKeys.orgIdentifier, changeEvent.getOrgIdentifier())
          .set(ChangeEventKeys.projectIdentifier, changeEvent.getProjectIdentifier())
          .set(ChangeEventKeys.serviceIdentifier, changeEvent.getServiceIdentifier())
          .set(ChangeEventKeys.envIdentifier, changeEvent.getEnvIdentifier())
          .set(ChangeEventKeys.eventTime, changeEvent.getEventTime())
          .set(ChangeEventKeys.type, changeEvent.getType());
    }
  }
}
