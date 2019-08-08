package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.trigger.WebhookSource.GitLabEventType;

import java.util.List;
import javax.validation.constraints.NotNull;

@JsonTypeName("GITLABS")
@Value
@Builder
public class GitLabsPayloadSource implements PayloadSource {
  @NotNull private Type type = Type.GITLABS;
  private List<GitLabEventType> gitLabEventTypes;
  private List<CustomPayloadExpression> customPayloadExpressions;
}
