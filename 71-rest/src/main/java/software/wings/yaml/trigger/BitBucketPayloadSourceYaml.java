package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.trigger.CustomPayloadExpression;
import software.wings.beans.trigger.PayloadSource.Type;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("BITBUCKET")
@JsonPropertyOrder({"harnessApiVersion"})
public class BitBucketPayloadSourceYaml extends PayloadSourceYaml {
  private List<WebhookEventYaml> events;
  private List<CustomPayloadExpression> customPayloadExpressions;

  BitBucketPayloadSourceYaml() {
    super.setType(Type.BITBUCKET.name());
  }

  @Builder
  BitBucketPayloadSourceYaml(List<WebhookEventYaml> events, List<CustomPayloadExpression> customPayloadExpressions) {
    super.setType(Type.BITBUCKET.name());
    this.events = events;
    this.customPayloadExpressions = customPayloadExpressions;
  }
}
