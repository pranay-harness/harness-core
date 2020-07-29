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
  // TODO: make it Duration and write a custom serializer
  private String duration;
  protected void populateCommonFields(VerificationJob verificationJob) {
    verificationJob.setIdentifier(this.identifier);
    verificationJob.setServiceIdentifier(serviceIdentifier);
    verificationJob.setEnvIdentifier(envIdentifier);
    verificationJob.setJobName(jobName);
    verificationJob.setDuration(parseDuration());
    verificationJob.setDataSources(dataSources);
  }

  private Duration parseDuration() {
    return Duration.ofMinutes(Integer.parseInt(duration.substring(0, duration.length() - 1)));
  }

  public abstract VerificationJob getVerificationJob();
  public abstract VerificationJobType getType();
}
