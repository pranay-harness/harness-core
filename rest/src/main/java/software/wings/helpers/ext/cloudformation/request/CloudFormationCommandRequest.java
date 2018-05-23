package software.wings.helpers.ext.cloudformation.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.AwsConfig;

@Data
@AllArgsConstructor
public class CloudFormationCommandRequest {
  @NotEmpty private CloudFormationCommandType commandType;
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;
  private AwsConfig awsConfig;
  private int timeoutInMs;
  private String region;
  public enum CloudFormationCommandType { CREATE_STACK, GET_STACKS, DELETE_STACK }
}