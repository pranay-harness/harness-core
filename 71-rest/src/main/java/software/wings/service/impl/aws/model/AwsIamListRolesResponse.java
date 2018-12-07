package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsIamListRolesResponse extends AwsResponse {
  private Map<String, String> roles;

  @Builder
  public AwsIamListRolesResponse(ExecutionStatus executionStatus, String errorMessage, Map<String, String> roles) {
    super(executionStatus, errorMessage);
    this.roles = roles;
  }
}