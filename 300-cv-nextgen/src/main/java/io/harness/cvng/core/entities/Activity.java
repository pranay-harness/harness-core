package io.harness.cvng.core.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@FieldNameConstants(innerTypeName = "ActivityKeys")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "activities")
@HarnessEntity(exportable = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class Activity implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;

  @NotNull private ActivityType type;
  @NotNull @FdIndex private String accountIdentifier;
  private String serviceIdentifier;
  @NotNull private String environmentIdentifier;
  @NotNull private String projectIdentifier;
  @NotNull private String orgIdentifier;

  private String activityName;
  private List<String> verificationJobsToTrigger;
  @NotNull private Instant activityStartTime;
  private Instant activityEndTime;

  @FdTtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  public abstract ActivityType getType();

  public enum ActivityType {
    DEPLOYMENT,
    INFRASTRUCTURE,
    CUSTOM;
  }
}
