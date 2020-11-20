package io.harness.notification.entities;

import io.harness.Team;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import static io.harness.Team.OTHER;

@Data
@Builder
@FieldNameConstants(innerTypeName = "NotificationKeys")
@Entity(value = "notifications", noClassnameStored = true)
@Document("notifications")
@TypeAlias("notifications")
public class Notification implements PersistentRegularIterable, PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String id;
  String accountIdentifier;
  @Builder.Default Team team = OTHER;

  Channel channel;

  @Builder.Default Boolean sent = Boolean.FALSE;
  @Builder.Default Integer retries = 0;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;
  @FdIndex private long nextIteration;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }
}
