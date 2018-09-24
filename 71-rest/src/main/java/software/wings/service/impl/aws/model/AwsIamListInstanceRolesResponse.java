package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

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