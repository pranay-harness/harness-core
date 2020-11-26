package io.harness.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.time.Duration.ofDays;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.plan.Plan;
import io.harness.pms.execution.Status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
@FieldNameConstants(innerTypeName = "PlanExecutionKeys")
@Entity(value = "planExecutions")
@Document("planExecutions")
@JsonIgnoreProperties(ignoreUnknown = true, value = {"plan"})
@TypeAlias("planExecution")
public class PlanExecution implements PersistentEntity, UuidAccess {
  public static final Duration TTL = ofDays(21);

  @Wither @Id @org.mongodb.morphia.annotations.Id String uuid;
  @Wither @CreatedDate Long createdAt;
  Plan plan;
  Map<String, String> setupAbstractions;
  @Default Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());

  Status status;
  Long startTs;
  Long endTs;

  @Wither @LastModifiedDate Long lastUpdatedAt;
  @Wither @Version Long version;
}
