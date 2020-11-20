package software.wings.helpers.ext.cloudformation.response;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.NameValuePair;

import java.util.List;

@Data
@Builder
public class CloudFormationRollbackInfo {
  private String body; // Used in Git / Template body case
  private String url; // Used in s3 case
  private String region;
  private String customStackName;
  private String cloudFormationRoleArn;
  private List<NameValuePair> variables;
}
