package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.trigger.CustomPayloadExpression;
import software.wings.beans.trigger.PayloadSource.Type;

import java.util.List;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("GITHUB")
@JsonPropertyOrder({"harnessApiVersion"})
public class GithubPayloadSourceYaml extends PayloadSourceYaml {
  private List<WebhookEventYaml> events;
  private List<CustomPayloadExpression> customPayloadExpressions;
  private List<String> filePaths;
  private String gitConnectorName;
  private String branchName;

  GithubPayloadSourceYaml() {
    super.setType(Type.GITHUB.name());
  }

  @Builder
  GithubPayloadSourceYaml(List<WebhookEventYaml> events, List<CustomPayloadExpression> customPayloadExpressions,
      List<String> filePaths, String gitConnectorName, String branchName) {
    super.setType(Type.GITHUB.name());
    this.events = events;
    this.customPayloadExpressions = customPayloadExpressions;
    this.filePaths = filePaths;
    this.gitConnectorName = gitConnectorName;
    this.branchName = branchName;
  }
}
