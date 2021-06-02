package io.harness.cvng.verificationjob.entities;

import static io.harness.cvng.CVConstants.DEFAULT_HEALTH_JOB_ID;
import static io.harness.cvng.CVConstants.DEFAULT_HEALTH_JOB_NAME;

import io.harness.cvng.beans.job.HealthVerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.utils.DateTimeUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@Data
@FieldNameConstants(innerTypeName = "HealthVerificationJobKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class HealthVerificationJob extends VerificationJob {
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.HEALTH;
  }

  @Override
  public VerificationJobDTO getVerificationJobDTO() {
    HealthVerificationJobDTO healthVerificationJobDTO = new HealthVerificationJobDTO();
    populateCommonFields(healthVerificationJobDTO);
    return healthVerificationJobDTO;
  }

  @Override
  public boolean shouldDoDataCollection() {
    return false;
  }

  @Override
  protected void validateParams() {}

  @Override
  public Optional<TimeRange> getPreActivityTimeRange(Instant deploymentStartTime) {
    return Optional.of(
        TimeRange.builder().startTime(deploymentStartTime.minus(getDuration())).endTime(deploymentStartTime).build());
  }

  @Override
  public Optional<TimeRange> getPostActivityTimeRange(Instant deploymentStartTime) {
    return Optional.of(
        TimeRange.builder().endTime(deploymentStartTime.plus(getDuration())).startTime(deploymentStartTime).build());
  }

  @Override
  public List<TimeRange> getDataCollectionTimeRanges(Instant startTime) {
    return getTimeRangesForDuration(startTime);
  }

  @Override
  public void fromDTO(VerificationJobDTO verificationJobDTO) {
    addCommonFileds(verificationJobDTO);
  }

  @Override
  public void resolveJobParams(Map<String, String> runtimeParameters) {}

  @Override
  public boolean collectHostData() {
    return false;
  }

  @Override
  public Instant roundToClosestBoundary(Instant deploymentStartTime, Instant startTime) {
    return DateTimeUtils.roundDownTo5MinBoundary(startTime);
  }

  @Override
  public Duration getExecutionDuration() {
    return getDuration().plus(getDuration());
  }

  @Override
  public Instant getAnalysisStartTime(Instant startTime) {
    return getPreActivityVerificationStartTime(startTime, null);
  }

  @Override
  public Instant eligibleToStartAnalysisTime(Instant startTime, Duration dataCollectionDelay, Instant createdAt) {
    // It is running as live monitoring so does not depend on dataCollectionDelay or createdAt
    return getAnalysisStartTime(startTime);
  }

  public static class HealthVerificationUpdatableEntity
      extends VerificationJobUpdatableEntity<HealthVerificationJob, HealthVerificationJobDTO> {
    @Override
    public void setUpdateOperations(
        UpdateOperations<HealthVerificationJob> updateOperations, HealthVerificationJobDTO dto) {
      setCommonOperations(updateOperations, dto);
    }
  }

  public Instant getPreActivityVerificationStartTime(Instant startTime, Instant preActivityStartTime) {
    // TODO: migration logic. Remove this after the release
    if (preActivityStartTime != null) {
      return preActivityStartTime;
    }
    return startTime.minus(getDuration());
  }

  public Instant getPostActivityVerificationStartTime(Instant startTime, Instant postActivityStartTime) {
    // TODO: remove this in the next release
    if (postActivityStartTime != null) {
      return postActivityStartTime;
    }
    return startTime;
  }

  public static HealthVerificationJob createDefaultJob(
      String accountId, String orgIdentifier, String projectIdentifier) {
    HealthVerificationJob verificationJob =
        HealthVerificationJob.builder().jobName(DEFAULT_HEALTH_JOB_NAME).identifier(DEFAULT_HEALTH_JOB_ID).build();
    VerificationJob.setDefaultJobCommonParameters(verificationJob, accountId, orgIdentifier, projectIdentifier);
    return verificationJob;
  }
}
