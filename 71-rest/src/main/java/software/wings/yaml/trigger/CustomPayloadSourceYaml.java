package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.trigger.PayloadSource.Type;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CUSTOM")
@JsonPropertyOrder({"harnessApiVersion"})
public class CustomPayloadSourceYaml extends PayloadSourceYaml {
  @Builder
  CustomPayloadSourceYaml() {
    super.setType(Type.CUSTOM.name());
  }
}
