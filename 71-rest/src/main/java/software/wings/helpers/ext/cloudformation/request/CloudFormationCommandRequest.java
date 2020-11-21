package software.wings.helpers.ext.cloudformation.request;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@AllArgsConstructor
public class CloudFormationCommandRequest implements ExecutionCapabilityDemander {
  @NotEmpty private CloudFormationCommandType commandType;
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;
  private AwsConfig awsConfig;
  private int timeoutInMs;
  private String region;
  private String cloudFormationRoleArn;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return awsConfig.fetchRequiredExecutionCapabilities();
  }

  public enum CloudFormationCommandType { CREATE_STACK, GET_STACKS, DELETE_STACK, UNKNOWN_REQUEST }
}
