package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.trigger.PayloadSource.Type;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
