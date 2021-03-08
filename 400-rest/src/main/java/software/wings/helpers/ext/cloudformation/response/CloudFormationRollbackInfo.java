package software.wings.helpers.ext.cloudformation.response;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.NameValuePair;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class CloudFormationRollbackInfo {
  private String body; // Used in Git / Template body case
  private String url; // Used in s3 case
  private String region;
  private String customStackName;
  private String cloudFormationRoleArn;
  private List<NameValuePair> variables;
}
