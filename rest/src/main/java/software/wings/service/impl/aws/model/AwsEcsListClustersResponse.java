package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEcsListClustersResponse extends AwsResponse {
  private List<String> clusters;

  @Builder
  public AwsEcsListClustersResponse(ExecutionStatus executionStatus, String errorMessage, List<String> clusters) {
    super(executionStatus, errorMessage);
    this.clusters = clusters;
  }
}