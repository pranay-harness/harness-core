package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAsgListAllNamesResponse extends AwsResponse {
  private List<String> aSgNames;

  @Builder
  public AwsAsgListAllNamesResponse(ExecutionStatus executionStatus, String errorMessage, List<String> aSgNames) {
    super(executionStatus, errorMessage);
    this.aSgNames = aSgNames;
  }
}