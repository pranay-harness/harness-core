package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.trigger.WebhookSource.GitHubEventType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@JsonTypeName("GITHUB")
@Value
@Builder
public class GitHubPayloadSource implements PayloadSource {
  @NotNull private Type type = Type.GITHUB;
  private List<GitHubEventType> gitHubEventTypes;
  private List<CustomPayloadExpression> customPayloadExpressions;
  private WebhookGitParam webhookGitParam;
}
