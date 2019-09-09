package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("WEBHOOK")
@JsonPropertyOrder({"harnessApiVersion"})
public class WebhookConditionYaml extends ConditionYaml {
  private List<PayloadSourceYaml> payloadSource;
}
