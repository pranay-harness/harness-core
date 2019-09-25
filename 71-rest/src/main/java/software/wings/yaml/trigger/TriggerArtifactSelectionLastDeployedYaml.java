package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.trigger.TriggerArtifactSelectionValue.ArtifactSelectionType;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("LAST_DEPLOYED")
@JsonPropertyOrder({"harnessApiVersion"})
public class TriggerArtifactSelectionLastDeployedYaml extends TriggerArtifactSelectionValueYaml {
  private String name;
  private String type;

  public TriggerArtifactSelectionLastDeployedYaml() {
    super.setType(ArtifactSelectionType.LAST_DEPLOYED.name());
  }

  @Builder
  public TriggerArtifactSelectionLastDeployedYaml(String name, String type) {
    super.setType(ArtifactSelectionType.LAST_DEPLOYED.name());
    this.name = name;
    this.type = type;
  }
}
