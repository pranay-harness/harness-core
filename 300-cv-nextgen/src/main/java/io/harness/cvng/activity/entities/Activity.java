package io.harness.cvng.activity.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import com.google.common.base.Preconditions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.ActivityDTO;
import io.harness.cvng.beans.ActivityDTO.VerificationJobRuntimeDetails;
import io.harness.cvng.beans.ActivityType;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
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
import java.util.ArrayList;
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
  private List<VerificationJobRuntimeDetails> verificationJobRuntimeDetails;
  @NotNull private Instant activityStartTime;
  private Instant activityEndTime;
  private List<String> verificationJobInstanceIds;
  private List<String> tags;
  @FdTtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  public abstract ActivityType getType();

  public abstract void fromDTO(ActivityDTO activityDTO);

  public abstract void fillInVerificationJobInstanceDetails(VerificationJobInstance verificationJobInstance);

  public void addCommonFileds(ActivityDTO activityDTO) {
    setAccountIdentifier(activityDTO.getAccountIdentifier());
    setProjectIdentifier(activityDTO.getProjectIdentifier());
    setOrgIdentifier(activityDTO.getOrgIdentifier());
    setServiceIdentifier(activityDTO.getServiceIdentifier());
    setEnvironmentIdentifier(activityDTO.getEnvironmentIdentifier());
    setActivityName(activityDTO.getName());
    setVerificationJobRuntimeDetails(activityDTO.getVerificationJobRuntimeDetails() == null
            ? null
            : new ArrayList<>(activityDTO.getVerificationJobRuntimeDetails()));
    setActivityStartTime(Instant.ofEpochMilli(activityDTO.getActivityStartTime()));
    setActivityEndTime(
        activityDTO.getActivityEndTime() != null ? Instant.ofEpochMilli(activityDTO.getActivityEndTime()) : null);
    setTags(activityDTO.getTags());
  }

  public abstract void validateActivityParams();

  public void validate() {
    Preconditions.checkNotNull(accountIdentifier, generateErrorMessageFromParam(ActivityKeys.accountIdentifier));
    Preconditions.checkNotNull(projectIdentifier, generateErrorMessageFromParam(ActivityKeys.projectIdentifier));
    Preconditions.checkNotNull(orgIdentifier, generateErrorMessageFromParam(ActivityKeys.orgIdentifier));
    Preconditions.checkNotNull(activityName, generateErrorMessageFromParam(ActivityKeys.activityName));
    Preconditions.checkNotNull(activityStartTime, generateErrorMessageFromParam(ActivityKeys.activityStartTime));
    this.validateActivityParams();
  }
}
