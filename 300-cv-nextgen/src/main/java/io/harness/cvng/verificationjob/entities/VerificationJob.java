package io.harness.cvng.verificationjob.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.DURATION_KEY;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.ENV_IDENTIFIER_KEY;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.SERVICE_IDENTIFIER_KEY;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@FieldNameConstants(innerTypeName = "VerificationJobKeys")
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "verificationJobs")
@HarnessEntity(exportable = true)
@SuperBuilder
// Also the serialization of duration is in millis.
public abstract class VerificationJob
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("query_idx")
                 .field(VerificationJobKeys.projectIdentifier)
                 .field(VerificationJobKeys.orgIdentifier)
                 .field(VerificationJobKeys.accountId)
                 .build())
        .build();
  }

  public VerificationJob() {
    this.type = getType();
  }
  @Id private String uuid;
  @NotNull private String identifier;
  @NotNull private String jobName;
  @FdIndex private long createdAt;
  @FdIndex private long lastUpdatedAt;
  private String projectIdentifier;
  private String orgIdentifier;
  private String activitySourceIdentifier;
  private VerificationJobType type;
  @NotNull private String accountId;
  @NotNull private RuntimeParameter serviceIdentifier;
  @NotNull private RuntimeParameter envIdentifier;
  @Deprecated private List<DataSourceType> dataSources;
  private List<String> monitoringSources;

  private RuntimeParameter duration;
  private boolean isDefaultJob;

  public abstract VerificationJobType getType();
  public abstract VerificationJobDTO getVerificationJobDTO();
  public abstract boolean shouldDoDataCollection();

  public void validate() {
    Preconditions.checkNotNull(accountId, generateErrorMessageFromParam(VerificationJobKeys.accountId));
    Preconditions.checkNotNull(identifier, generateErrorMessageFromParam(VerificationJobKeys.identifier));
    Preconditions.checkNotNull(jobName, generateErrorMessageFromParam(VerificationJobKeys.jobName));
    Preconditions.checkNotNull(serviceIdentifier, generateErrorMessageFromParam(VerificationJobKeys.serviceIdentifier));
    Preconditions.checkNotNull(projectIdentifier, generateErrorMessageFromParam(VerificationJobKeys.projectIdentifier));
    Preconditions.checkNotNull(orgIdentifier, generateErrorMessageFromParam(VerificationJobKeys.orgIdentifier));
    Preconditions.checkNotNull(envIdentifier, generateErrorMessageFromParam(VerificationJobKeys.envIdentifier));
    Preconditions.checkNotNull(duration, generateErrorMessageFromParam(VerificationJobKeys.duration));
    Preconditions.checkNotNull(monitoringSources, generateErrorMessageFromParam(VerificationJobKeys.monitoringSources));
    Preconditions.checkArgument(!monitoringSources.isEmpty(), "Monitoring Sources can not be empty");
    if (!duration.isRuntimeParam()) {
      Preconditions.checkArgument(getDuration().toMinutes() >= 5,
          "Minimum allowed duration is 5 mins. Current value(mins): %s", getDuration().toMinutes());
    }
    Preconditions.checkNotNull(type, generateErrorMessageFromParam(VerificationJobKeys.type));
    this.validateParams();
  }

  protected abstract void validateParams();
  // TODO: Should this time range be configurable ?
  public abstract Optional<TimeRange> getPreActivityTimeRange(Instant deploymentStartTime);
  public abstract Optional<TimeRange> getPostActivityTimeRange(Instant deploymentStartTime);
  public abstract List<TimeRange> getDataCollectionTimeRanges(Instant startTime);

  protected void populateCommonFields(VerificationJobDTO verificationJobDTO) {
    verificationJobDTO.setIdentifier(this.identifier);
    verificationJobDTO.setJobName(this.jobName);
    verificationJobDTO.setDuration(this.duration.isRuntimeParam() ? "${duration}" : this.duration.getValue());

    verificationJobDTO.setServiceIdentifier(
        this.serviceIdentifier.isRuntimeParam() ? "${serviceIdentifier}" : (String) serviceIdentifier.getValue());
    verificationJobDTO.setEnvIdentifier(
        this.envIdentifier.isRuntimeParam() ? "${envIdentifier}" : (String) envIdentifier.getValue());
    verificationJobDTO.setDataSources(this.dataSources);
    verificationJobDTO.setProjectIdentifier(this.getProjectIdentifier());
    verificationJobDTO.setOrgIdentifier(this.getOrgIdentifier());
    verificationJobDTO.setDefaultJob(isDefaultJob);
    verificationJobDTO.setActivitySourceIdentifier(activitySourceIdentifier);
    verificationJobDTO.setMonitoringSources(monitoringSources);
    verificationJobDTO.setVerificationJobUrl(getVerificationJobUrl());
  }

  public abstract void fromDTO(VerificationJobDTO verificationJobDTO);

  public void addCommonFileds(VerificationJobDTO verificationJobDTO) {
    this.setIdentifier(verificationJobDTO.getIdentifier());
    this.setServiceIdentifier(verificationJobDTO.getServiceIdentifier(),
        VerificationJobDTO.isRuntimeParam(verificationJobDTO.getServiceIdentifier()));
    this.setEnvIdentifier(verificationJobDTO.getEnvIdentifier(),
        VerificationJobDTO.isRuntimeParam(verificationJobDTO.getEnvIdentifier()));
    this.setJobName(verificationJobDTO.getJobName());
    this.setDuration(
        verificationJobDTO.getDuration(), VerificationJobDTO.isRuntimeParam(verificationJobDTO.getDuration()));
    this.setDataSources(verificationJobDTO.getDataSources());
    this.setMonitoringSources(verificationJobDTO.getMonitoringSources());
    this.setProjectIdentifier(verificationJobDTO.getProjectIdentifier());
    this.setOrgIdentifier(verificationJobDTO.getOrgIdentifier());
    this.setActivitySourceIdentifier(verificationJobDTO.getActivitySourceIdentifier());
    this.setType(verificationJobDTO.getType());
    this.setDefaultJob(verificationJobDTO.isDefaultJob());
  }

  protected List<TimeRange> getTimeRangesForDuration(Instant startTime) {
    Preconditions.checkArgument(
        !duration.isRuntimeParam(), "Duration is marked as a runtime arg that hasn't been resolved yet.");
    List<TimeRange> ranges = new ArrayList<>();
    for (Instant current = startTime; current.compareTo(startTime.plusMillis(getDuration().toMillis())) < 0;
         current = current.plus(1, ChronoUnit.MINUTES)) {
      ranges.add(TimeRange.builder().startTime(current).endTime(current.plus(1, ChronoUnit.MINUTES)).build());
    }
    return ranges;
  }

  public String getVerificationJobUrl() {
    URIBuilder jobUrlBuilder = new URIBuilder();
    jobUrlBuilder.setPath("/verification-job");
    jobUrlBuilder.addParameter(VerificationJobKeys.accountId, accountId);
    jobUrlBuilder.addParameter(VerificationJobKeys.orgIdentifier, orgIdentifier);
    jobUrlBuilder.addParameter(VerificationJobKeys.projectIdentifier, projectIdentifier);
    jobUrlBuilder.addParameter(VerificationJobKeys.identifier, identifier);

    try {
      return jobUrlBuilder.build().toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  public void setDuration(Duration duration) {
    this.duration = RuntimeParameter.builder().isRuntimeParam(false).value(duration.toMinutes() + "m").build();
  }

  public void setDuration(String duration, boolean isRuntimeParam) {
    this.duration =
        duration == null ? null : RuntimeParameter.builder().isRuntimeParam(isRuntimeParam).value(duration).build();
  }

  public Duration getDuration() {
    if (duration.isRuntimeParam()) {
      return null;
    }
    return Duration.ofMinutes(Integer.parseInt(duration.getValue().substring(0, duration.getValue().length() - 1)));
  }

  public String getServiceIdentifier() {
    return serviceIdentifier.getValue();
  }

  public void setServiceIdentifier(String serviceIdentifier, boolean isRuntimeParam) {
    this.serviceIdentifier = serviceIdentifier == null
        ? null
        : RuntimeParameter.builder().isRuntimeParam(isRuntimeParam).value(serviceIdentifier).build();
  }

  public void setEnvIdentifier(String envIdentifier, boolean isRuntimeParam) {
    this.envIdentifier = envIdentifier == null
        ? null
        : RuntimeParameter.builder().isRuntimeParam(isRuntimeParam).value(envIdentifier).build();
  }

  public String getEnvIdentifier() {
    return envIdentifier.getValue();
  }

  public RuntimeParameter getEnvIdentifierRuntimeParam() {
    return envIdentifier;
  }

  public RuntimeParameter getServiceIdentifierRuntimeParam() {
    return serviceIdentifier;
  }

  public abstract void resolveJobParams(Map<String, String> runtimeParameters);
  public VerificationJob resolveAdditionsFields(VerificationJobInstanceService verificationJobInstanceService) {
    // no-op by default. Designed to override.
    return this;
  }

  public VerificationJob resolveVerificationJob(Map<String, String> runtimeParameters) {
    if (isNotEmpty(runtimeParameters)) {
      runtimeParameters.keySet().forEach(key -> {
        switch (key) {
          case SERVICE_IDENTIFIER_KEY:
            this.setServiceIdentifier(runtimeParameters.get(key), false);
            break;
          case ENV_IDENTIFIER_KEY:
            this.setEnvIdentifier(runtimeParameters.get(key), false);
            break;
          case DURATION_KEY:
            this.setDuration(runtimeParameters.get(key), false);
            break;
          default:
            break;
        }
      });
    }
    this.resolveJobParams(runtimeParameters);
    /* Null is set on uuid because morphia is loading the value from verificationJobs collection if uuid is present in
    some cases.

    VerificationJobInstanceServiceImpl.class
    hPersistence.createQuery(VerificationJobInstance.class,excludeAuthority).field(VerificationJobInstanceKeys.uuid).in(verificationJobInstanceIds).asList()
     */
    setUuid(null);
    return this;
  }

  public abstract boolean collectHostData();

  @FieldNameConstants(innerTypeName = "RuntimeParameterKeys")
  @Data
  @Builder
  public static class RuntimeParameter {
    boolean isRuntimeParam;
    String value;

    public String string() {
      if (isRuntimeParam) {
        return "${input}";
      }
      return value;
    }
  }
}
