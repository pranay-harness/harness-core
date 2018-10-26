package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAmiServiceSetupResponse extends AwsResponse {
  private String newAsgName;
  private String lastDeployedAsgName;
  private Integer harnessRevision;
  private List<String> oldAsgNames;
  private AwsAmiPreDeploymentData preDeploymentData;
  private boolean blueGreen;

  @Builder
  public AwsAmiServiceSetupResponse(ExecutionStatus executionStatus, String errorMessage, String newAsgName,
      String lastDeployedAsgName, Integer harnessRevision, AwsAmiPreDeploymentData preDeploymentData,
      List<String> oldAsgNames, boolean blueGreen) {
    super(executionStatus, errorMessage);
    this.newAsgName = newAsgName;
    this.lastDeployedAsgName = lastDeployedAsgName;
    this.harnessRevision = harnessRevision;
    this.preDeploymentData = preDeploymentData;
    this.oldAsgNames = oldAsgNames;
    this.blueGreen = blueGreen;
  }
}