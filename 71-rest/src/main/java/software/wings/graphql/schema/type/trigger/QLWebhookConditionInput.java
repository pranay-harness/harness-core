package software.wings.graphql.schema.type.trigger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLWebhookConditionInput {
  QLWebhookSource webhookSourceType;
  QLGitHubEvent githubEvent;
  QLBitbucketEvent bitbucketEvent;
  QLGitlabEvent gitlabEvent;
  String branchRegex;
  String branchName;
  String repoName;
  Boolean deployOnlyIfFilesChanged;
  List<String> filePaths;
  String gitConnectorId;
}
