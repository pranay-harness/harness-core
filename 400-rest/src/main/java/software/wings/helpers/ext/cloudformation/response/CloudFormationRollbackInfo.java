package software.wings.helpers.ext.cloudformation.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.NameValuePair;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class CloudFormationRollbackInfo {
  private String body; // Used in Git / Template body case
  private String url; // Used in s3 case
  private String region;
  private String customStackName;
  private String cloudFormationRoleArn;
  private boolean skipBasedOnStackStatus;
  private List<String> stackStatusesToMarkAsSuccess;
  private List<NameValuePair> variables;
}
