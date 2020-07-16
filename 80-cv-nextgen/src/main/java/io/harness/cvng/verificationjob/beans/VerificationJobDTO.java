package io.harness.cvng.verificationjob.beans;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.List;

@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class VerificationJobDTO {
  private String identifier;
  private String jobName;
  private String serviceIdentifier;
  private String envIdentifier;
  private List<DataSourceType> dataSources;
  private Duration duration;
  public void populateCommonFields(VerificationJob verificationJob) {
    verificationJob.setIdentifier(this.identifier);
    verificationJob.setServiceIdentifier(serviceIdentifier);
    verificationJob.setEnvIdentifier(envIdentifier);
    verificationJob.setJobName(jobName);
    verificationJob.setDuration(duration);
    verificationJob.setDataSources(dataSources);
  }

  public abstract VerificationJob getVerificationJob();
  public abstract VerificationJobType getType();
}
