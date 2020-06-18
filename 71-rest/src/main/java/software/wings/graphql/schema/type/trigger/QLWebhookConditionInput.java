package software.wings.graphql.schema.type.trigger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLWebhookConditionInput {
  private QLWebhookSource webhookSourceType;
  private QLGitHubEvent githubEvent;
  private QLBitbucketEvent bitbucketEvent;
  private QLGitlabEvent gitlabEvent;
  private String branchRegex;
}
