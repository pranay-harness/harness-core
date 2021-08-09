package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.trigger.CustomPayloadExpression;
import software.wings.beans.trigger.PayloadSource.Type;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("BITBUCKET")
@JsonPropertyOrder({"harnessApiVersion"})

public class GitlabPayloadSourceYaml extends PayloadSourceYaml {
  private List<WebhookEventYaml> events;
  private List<CustomPayloadExpression> customPayloadExpressions;

  GitlabPayloadSourceYaml() {
    super.setType(Type.GITLAB.name());
  }

  @Builder
  GitlabPayloadSourceYaml(List<WebhookEventYaml> events, List<CustomPayloadExpression> customPayloadExpressions) {
    super.setType(Type.GITLAB.name());
    this.events = events;
    this.customPayloadExpressions = customPayloadExpressions;
  }
}
