package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.time.Duration.ofDays;

import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Data
@Builder
@FieldNameConstants(innerTypeName = "TimeoutInstanceKeys")
@Entity(value = "timeoutInstances")
@Document("timeoutInstances")
public class TimeoutInstance implements PersistentRegularIterable, UuidAccess {
  public static final Duration TTL = ofDays(21);

  @Id @org.mongodb.morphia.annotations.Id String uuid;
  @NotNull TimeoutTracker tracker;
  @NotNull TimeoutCallback callback;

  @FdIndex @CreatedDate Long createdAt;
  @LastModifiedDate Long lastUpdatedAt;
  @Version Long version;

  @Builder.Default @FdTtlIndex Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());

  // nextIteration is the approximate expiry time. If the expiry time is known, nextIteration equals expiry time. If the
  // tracker is paused, this is set to a very high value. When the tracker resumes, we update the value and wakeup the
  // iterator.
  @FdIndex long nextIteration;

  public void resetNextIteration() {
    Long expiryTime = tracker.getExpiryTime();
    nextIteration = expiryTime == null ? Long.MAX_VALUE : expiryTime;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }
}
