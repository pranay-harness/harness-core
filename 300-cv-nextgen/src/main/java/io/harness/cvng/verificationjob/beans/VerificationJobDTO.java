package io.harness.cvng.verificationjob.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.verificationjob.entities.VerificationJob;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@SuperBuilder
public abstract class VerificationJobDTO {
  private String identifier;
  private String jobName;
  private String serviceIdentifier;
  private String envIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;
  private String activitySourceIdentifier;
  private List<DataSourceType> dataSources;
  // TODO: make it Duration and write a custom serializer
  private String duration;
  private boolean isDefaultJob;

  protected void populateCommonFields(VerificationJob verificationJob) {
    verificationJob.setIdentifier(this.identifier);
    verificationJob.setServiceIdentifier(serviceIdentifier, isRuntimeParam(serviceIdentifier));
    verificationJob.setEnvIdentifier(envIdentifier, isRuntimeParam(envIdentifier));
    verificationJob.setJobName(jobName);
    verificationJob.setDuration(duration, isRuntimeParam(duration));
    verificationJob.setDataSources(dataSources);
    verificationJob.setProjectIdentifier(projectIdentifier);
    verificationJob.setOrgIdentifier(orgIdentifier);
    verificationJob.setActivitySourceIdentifier(activitySourceIdentifier);
    verificationJob.setType(getType());
    verificationJob.setDefaultJob(isDefaultJob);
  }

  @JsonIgnore public abstract VerificationJob getVerificationJob();
  public abstract VerificationJobType getType();
  @JsonIgnore
  protected boolean isRuntimeParam(String value) {
    return isNotEmpty(value) && value.startsWith("${") && value.endsWith("}");
  }
}
