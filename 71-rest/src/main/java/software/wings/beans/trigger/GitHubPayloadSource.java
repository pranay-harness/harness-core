package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.trigger.WebhookSource.GitHubEventType;

import java.util.List;
import javax.validation.constraints.NotNull;

@JsonTypeName("GITHUB")
@Value
@Builder
public class GitHubPayloadSource implements PayloadSource {
  @NotNull private Type type = Type.GITHUB;
  private List<GitHubEventType> gitHubEventTypes;
  private List<CustomPayloadExpression> customPayloadExpressions;
  private WebhookGitParam webhookGitParam;
}
