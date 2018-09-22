package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAmiServiceSetupResponse extends AwsResponse {
  private String newAsgName;
  private String lastDeployedAsgName;
  private Integer harnessRevision;
  private int lastDeployedAsgMinSize;
  private int lastDeployedAsgDesiredCapacity;

  @Builder
  public AwsAmiServiceSetupResponse(ExecutionStatus executionStatus, String errorMessage, String newAsgName,
      String lastDeployedAsgName, Integer harnessRevision, int lastDeployedAsgMinSize,
      int lastDeployedAsgDesiredCapacity) {
    super(executionStatus, errorMessage);
    this.newAsgName = newAsgName;
    this.lastDeployedAsgName = lastDeployedAsgName;
    this.harnessRevision = harnessRevision;
    this.lastDeployedAsgMinSize = lastDeployedAsgMinSize;
    this.lastDeployedAsgDesiredCapacity = lastDeployedAsgDesiredCapacity;
  }
}