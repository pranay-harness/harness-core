package software.wings.helpers.ext.cloudformation.request;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.AwsConfig;

import java.util.List;

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

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return awsConfig.fetchRequiredExecutionCapabilities();
  }

  public enum CloudFormationCommandType { CREATE_STACK, GET_STACKS, DELETE_STACK }
}