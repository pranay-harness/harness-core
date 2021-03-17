package software.wings.graphql.schema.type.trigger;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(Module._380_CG_GRAPHQL)
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
