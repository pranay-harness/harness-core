package io.harness.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.time.Duration.ofDays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.execution.status.ExecutionInstanceStatus;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAccess;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAccess;
import io.harness.plan.Plan;
import io.harness.plan.input.InputArgs;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Data
@Builder
@Redesign
@FieldNameConstants(innerTypeName = "PlanExecutionKeys")
@Entity(value = "planExecutions")
@JsonIgnoreProperties(ignoreUnknown = true, value = {"plan"})
public final class PlanExecution implements PersistentRegularIterable, CreatedAtAware, CreatedAtAccess, CreatedByAware,
                                            CreatedByAccess, UpdatedAtAware, UuidAccess {
  public static final Duration TTL = ofDays(21);

  @Id @NotNull private String uuid;
  private EmbeddedUser createdBy;
  private long createdAt;
  private Plan plan;
  private InputArgs inputArgs;
  private Long nextIteration;
  @Builder.Default private Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());

  ExecutionInstanceStatus status;
  private Long startTs;
  private Long endTs;

  long lastUpdatedAt;

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }
}
