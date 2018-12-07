package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsIamListInstanceRolesResponse extends AwsResponse {
  private List<String> instanceRoles;

  @Builder
  public AwsIamListInstanceRolesResponse(
      ExecutionStatus executionStatus, String errorMessage, List<String> instanceRoles) {
    super(executionStatus, errorMessage);
    this.instanceRoles = instanceRoles;
  }
}