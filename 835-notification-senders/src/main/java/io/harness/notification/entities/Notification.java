package io.harness.notification.entities;

import static io.harness.Team.OTHER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.Team;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "NotificationKeys")
@Entity(value = "notificationsNg", noClassnameStored = true)
@Document("notificationsNg")
@TypeAlias("notificationsNg")
@OwnedBy(PL)
@StoreIn(DbAliases.NOTIFICATION)
public class Notification implements PersistentRegularIterable, PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_notification_idx")
                 .unique(true)
                 .field(NotificationKeys.id)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("notification_retries_sent")
                 .field(NotificationKeys.shouldRetry)
                 .field(NotificationKeys.retries)
                 .build())
        .build();
  }
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String id;
  String accountIdentifier;
  @Builder.Default Team team = OTHER;

  Channel channel;

  List<Boolean> processingResponses;
  @Builder.Default boolean shouldRetry = Boolean.TRUE;
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
