package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.trigger.PayloadSource.Type;

@OwnedBy(CDC)
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
