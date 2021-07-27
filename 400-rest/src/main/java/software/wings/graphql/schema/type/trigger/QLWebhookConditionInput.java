package software.wings.graphql.schema.type.trigger;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
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
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
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
  String webhookSecret;
}
