package io.harness.cvng.beans.change;

import io.harness.cvng.beans.activity.ActivityVerificationStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarnessCDEventMetadata extends ChangeEventMetadata {
  long deploymentStartTime;
  long deploymentEndTime;
  String planExecutionId;
  String pipelineId;
  String stageStepId;
  String stageId;
  String artifactType;
  String artifactTag;
  String status;
  List<VerifyStepSummary> verifyStepSummaries;
  String pipelinePath;

  @Override
  public ChangeSourceType getType() {
    return ChangeSourceType.HARNESS_CD;
  }

  @Builder
  public static class VerifyStepSummary {
    String name;
    ActivityVerificationStatus verificationStatus;
  }
}