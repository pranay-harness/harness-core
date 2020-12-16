package io.harness.cvng.activity.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityDTO.VerificationJobRuntimeDetails;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@FieldNameConstants(innerTypeName = "ActivityKeys")
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "activities")
@HarnessEntity(exportable = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class Activity implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("deployment_query_idx")
                 .field(ActivityKeys.accountIdentifier)
                 .field(ActivityKeys.orgIdentifier)
                 .field(ActivityKeys.projectIdentifier)
                 .field(ActivityKeys.type)
                 .build(),
            CompoundMongoIndex.builder()
                .name("deployment_tag_idx")
                .field(ActivityKeys.accountIdentifier)
                .field(ActivityKeys.type)
                .field(ActivityKeys.verificationJobInstanceIds)
                .build(),
            CompoundMongoIndex.builder()
                .name("activities_query_index")
                .field(ActivityKeys.accountIdentifier)
                .field(ActivityKeys.orgIdentifier)
                .field(ActivityKeys.projectIdentifier)
                .field(ActivityKeys.activityStartTime)
                .build())
        .build();
  }

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
  @FdIndex private List<String> verificationJobInstanceIds;
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
