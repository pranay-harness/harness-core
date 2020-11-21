package io.harness.batch.processing.billing.tasklet.entities;

import static io.harness.event.app.EventServiceApplication.EVENTS_DB;

import io.harness.annotation.StoreIn;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.checkerframework.common.aliasing.qual.Unique;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@Entity(value = "dataGeneratedNotification", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "DataGeneratedNotificationKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn(EVENTS_DB)
public final class DataGeneratedNotification implements PersistentEntity, UuidAware, CreatedAtAware, AccountAccess {
  @Id String uuid;
  @Unique String accountId;
  boolean mailSent;
  List<String> clusterIds;
  long createdAt;
}
