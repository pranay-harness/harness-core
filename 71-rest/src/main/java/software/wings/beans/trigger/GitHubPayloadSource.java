package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.trigger.WebhookSource.GitHubEventType;

import java.util.List;
import javax.validation.constraints.NotNull;

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
