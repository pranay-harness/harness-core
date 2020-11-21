package software.wings.yaml.trigger;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CDC)
@Data
@JsonTypeName("NEW_MANIFEST")
@Builder
public class ManifestTriggerConditionYaml extends TriggerConditionYaml {
  private String serviceName;
  private String versionRegex;

  public ManifestTriggerConditionYaml() {
    super.setType("NEW_MANIFEST");
  }

  public ManifestTriggerConditionYaml(String serviceName, String versionRegex) {
    this();
    this.serviceName = serviceName;
    this.versionRegex = versionRegex;
  }
}
