package io.harness.cvng.verificationjob.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class VerificationJobDTO {
  private String identifier;
  private String jobName;
  private String serviceIdentifier;
  private String envIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;
  private List<DataSourceType> dataSources;
  // TODO: make it Duration and write a custom serializer
  private String duration;
  protected void populateCommonFields(VerificationJob verificationJob) {
    verificationJob.setIdentifier(this.identifier);
    verificationJob.setServiceIdentifier(serviceIdentifier, isRuntimeParam(serviceIdentifier));
    verificationJob.setEnvIdentifier(envIdentifier, isRuntimeParam(envIdentifier));
    verificationJob.setJobName(jobName);
    verificationJob.setDuration(duration, isRuntimeParam(duration));
    verificationJob.setDataSources(dataSources);
    verificationJob.setProjectIdentifier(projectIdentifier);
    verificationJob.setOrgIdentifier(orgIdentifier);
    verificationJob.setType(getType());
  }

  @JsonIgnore public abstract VerificationJob getVerificationJob();
  public abstract VerificationJobType getType();
  @JsonIgnore
  protected boolean isRuntimeParam(String value) {
    return isNotEmpty(value) && value.startsWith("${") && value.endsWith("}");
  }
}
