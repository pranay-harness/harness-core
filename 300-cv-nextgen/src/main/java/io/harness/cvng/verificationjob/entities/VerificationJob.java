package io.harness.cvng.verificationjob.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import com.google.common.base.Preconditions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobType;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.persistence.converters.DurationConverter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@FieldNameConstants(innerTypeName = "VerificationJobKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "verificationJobs")
@HarnessEntity(exportable = true)
// TODO: find a way to add it to using morphia.getMapper().getConverters().addConverter(new DurationConverter());
// Also the serialization of duration is in millis.
@Converters({DurationConverter.class})
public abstract class VerificationJob
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id private String uuid;
  private String identifier;
  private String jobName;
  private long createdAt;
  private long lastUpdatedAt;
  private String projectIdentifier;
  private String orgIdentifier;
  @NotNull @FdIndex private String accountId;
  @NotNull private String serviceIdentifier;
  @NotNull private String envIdentifier;
  private List<DataSourceType> dataSources;
  private Duration duration;
  public abstract VerificationJobType getType();
  public abstract VerificationJobDTO getVerificationJobDTO();
  public void validate() {
    Preconditions.checkNotNull(accountId, generateErrorMessageFromParam(VerificationJobKeys.accountId));
    Preconditions.checkNotNull(identifier, generateErrorMessageFromParam(VerificationJobKeys.identifier));
    Preconditions.checkNotNull(jobName, generateErrorMessageFromParam(VerificationJobKeys.jobName));
    Preconditions.checkNotNull(serviceIdentifier, generateErrorMessageFromParam(VerificationJobKeys.serviceIdentifier));
    Preconditions.checkNotNull(serviceIdentifier, generateErrorMessageFromParam(VerificationJobKeys.serviceIdentifier));
    Preconditions.checkNotNull(projectIdentifier, generateErrorMessageFromParam(VerificationJobKeys.projectIdentifier));
    Preconditions.checkNotNull(orgIdentifier, generateErrorMessageFromParam(VerificationJobKeys.orgIdentifier));
    Preconditions.checkNotNull(envIdentifier, generateErrorMessageFromParam(VerificationJobKeys.envIdentifier));
    Preconditions.checkNotNull(duration, generateErrorMessageFromParam(VerificationJobKeys.duration));
    Preconditions.checkNotNull(dataSources, generateErrorMessageFromParam(VerificationJobKeys.dataSources));
    Preconditions.checkArgument(!dataSources.isEmpty(), "DataSources can not be empty");
    Preconditions.checkArgument(
        duration.toMinutes() >= 5, "Minimum allowed duration is 5 mins. Current value(ms): %s", duration.toMillis());
    this.validateParams();
  }

  protected abstract void validateParams();
  public abstract TimeRange getPreDeploymentTimeRange(Instant deploymentStartTime);
  public abstract List<TimeRange> getDataCollectionTimeRanges(Instant startTime);
  protected void populateCommonFields(VerificationJobDTO verificationJobDTO) {
    verificationJobDTO.setIdentifier(this.identifier);
    verificationJobDTO.setJobName(this.jobName);
    verificationJobDTO.setDuration(this.duration.toMinutes() + "m");
    verificationJobDTO.setServiceIdentifier(this.serviceIdentifier);
    verificationJobDTO.setEnvIdentifier(this.envIdentifier);
    verificationJobDTO.setDataSources(this.dataSources);
    verificationJobDTO.setProjectIdentifier(this.getProjectIdentifier());
    verificationJobDTO.setOrgIdentifier(this.getOrgIdentifier());
  }

  protected List<TimeRange> getTimeRangesForDuration(Instant startTime) {
    List<TimeRange> ranges = new ArrayList<>();
    for (Instant current = startTime; current.compareTo(startTime.plusMillis(getDuration().toMillis())) < 0;
         current = current.plus(1, ChronoUnit.MINUTES)) {
      ranges.add(TimeRange.builder().startTime(current).endTime(current.plus(1, ChronoUnit.MINUTES)).build());
    }
    return ranges;
  }
}
