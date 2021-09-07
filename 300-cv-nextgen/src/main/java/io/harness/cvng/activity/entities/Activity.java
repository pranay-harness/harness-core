package io.harness.cvng.activity.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.cvng.activity.beans.ActivityVerificationSummary;
import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityDTO.VerificationJobRuntimeDetails;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.core.services.api.UpdatableEntity;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceBuilder;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Data
@FieldNameConstants(innerTypeName = "ActivityKeys")
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "activities")
@HarnessEntity(exportable = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@StoreIn(DbAliases.CVNG)
public abstract class Activity
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, PersistentRegularIterable {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("deployment_query_idx")
                 .field(ActivityKeys.accountId)
                 .field(ActivityKeys.orgIdentifier)
                 .field(ActivityKeys.projectIdentifier)
                 .field(ActivityKeys.type)
                 .build(),
            CompoundMongoIndex.builder()
                .name("deployment_tag_idx")
                .field(ActivityKeys.accountId)
                .field(ActivityKeys.type)
                .field(ActivityKeys.verificationJobInstanceIds)
                .build(),
            CompoundMongoIndex.builder()
                .name("activities_query_index")
                .field(ActivityKeys.accountId)
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
  @NotNull private String accountId;
  private String serviceIdentifier;
  @NotNull private String environmentIdentifier;
  @NotNull private String projectIdentifier;
  @NotNull private String orgIdentifier;
  private String activitySourceId;

  private String changeSourceIdentifier;
  private Instant eventTime;

  private String activityName;
  @Deprecated private List<VerificationJobRuntimeDetails> verificationJobRuntimeDetails;
  private List<VerificationJob> verificationJobs;

  @NotNull private Instant activityStartTime;
  private Instant activityEndTime;
  @FdIndex private List<String> verificationJobInstanceIds;
  private List<String> tags;
  @Builder.Default @FdTtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  private ActivityVerificationSummary verificationSummary;

  @Builder.Default private ActivityVerificationStatus analysisStatus = ActivityVerificationStatus.NOT_STARTED;

  @FdIndex private Long verificationIteration;

  public abstract ActivityType getType();

  public List<VerificationJob> getVerificationJobs() {
    if (verificationJobs == null) {
      verificationJobs = Collections.EMPTY_LIST;
    }
    return verificationJobs;
  }

  public Instant getEventTime() {
    if (eventTime == null) {
      eventTime = this.activityStartTime;
    }
    return eventTime;
  }

  public abstract void fromDTO(ActivityDTO activityDTO);

  public abstract void fillInVerificationJobInstanceDetails(
      VerificationJobInstanceBuilder verificationJobInstanceBuilder);

  protected void addCommonFields(ActivityDTO activityDTO) {
    setAccountId(activityDTO.getAccountIdentifier());
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
    setType(activityDTO.getType());
    setTags(activityDTO.getTags());
  }

  public abstract void validateActivityParams();

  public void validate() {
    Preconditions.checkNotNull(accountId, generateErrorMessageFromParam(ActivityKeys.accountId));
    Preconditions.checkNotNull(projectIdentifier, generateErrorMessageFromParam(ActivityKeys.projectIdentifier));
    Preconditions.checkNotNull(orgIdentifier, generateErrorMessageFromParam(ActivityKeys.orgIdentifier));
    Preconditions.checkNotNull(activityName, generateErrorMessageFromParam(ActivityKeys.activityName));
    Preconditions.checkNotNull(activityStartTime, generateErrorMessageFromParam(ActivityKeys.activityStartTime));
    Preconditions.checkNotNull(type, generateErrorMessageFromParam(ActivityKeys.type));
    this.validateActivityParams();
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (fieldName.equals(ActivityKeys.verificationIteration)) {
      this.verificationIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (fieldName.equals(ActivityKeys.verificationIteration)) {
      return verificationIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public abstract boolean deduplicateEvents();

  public abstract static class ActivityUpdatableEntity<T extends Activity, D extends Activity>
      implements UpdatableEntity<T, D> {
    public abstract Class getEntityClass();

    public Query<T> populateKeyQuery(Query<T> query, D activity) {
      return query.filter(ActivityKeys.orgIdentifier, activity.getOrgIdentifier())
          .filter(ActivityKeys.projectIdentifier, activity.getProjectIdentifier())
          .filter(ActivityKeys.serviceIdentifier, activity.getServiceIdentifier())
          .filter(ActivityKeys.environmentIdentifier, activity.getEnvironmentIdentifier());
    }

    protected void setCommonUpdateOperations(UpdateOperations<T> updateOperations, D activity) {
      updateOperations.set(ActivityKeys.accountId, activity.getAccountId())
          .set(ActivityKeys.orgIdentifier, activity.getOrgIdentifier())
          .set(ActivityKeys.projectIdentifier, activity.getProjectIdentifier())
          .set(ActivityKeys.serviceIdentifier, activity.getServiceIdentifier())
          .set(ActivityKeys.environmentIdentifier, activity.getEnvironmentIdentifier())
          .set(ActivityKeys.eventTime, activity.getEventTime())
          .set(ActivityKeys.activityStartTime, activity.getActivityStartTime())
          .set(ActivityKeys.activityEndTime, activity.getActivityEndTime())
          .set(ActivityKeys.changeSourceIdentifier, activity.getChangeSourceIdentifier())
          .set(ActivityKeys.type, activity.getType());
    }
  }
}
