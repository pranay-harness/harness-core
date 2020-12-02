package io.harness.cvng.verificationjob.beans;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.verificationjob.entities.CanaryVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("CANARY")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class CanaryVerificationJobDTO extends VerificationJobDTO {
  private String sensitivity;
  private Integer trafficSplitPercentage;
  @Override
  public VerificationJob getVerificationJob() {
    CanaryVerificationJob canaryVerificationJob = new CanaryVerificationJob();
    canaryVerificationJob.setSensitivity(sensitivity, isRuntimeParam(sensitivity));
    canaryVerificationJob.setTrafficSplitPercentage(trafficSplitPercentage);
    populateCommonFields(canaryVerificationJob);
    return canaryVerificationJob;
  }

  @Override
  public VerificationJobType getType() {
    return VerificationJobType.CANARY;
  }

  public static void main(String[] args) throws JsonProcessingException {
    CanaryVerificationJobDTO build = CanaryVerificationJobDTO.builder()
                                         .identifier("canary")
                                         .jobName("canary")
                                         .serviceIdentifier("todolist")
                                         .envIdentifier("Prod")
                                         .projectIdentifier("raghu_p")
                                         .orgIdentifier("harness_test")
                                         .dataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS))
                                         .build();
    System.out.println(JsonUtils.asPrettyJson(build));
  }
}
