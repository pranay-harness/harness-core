package io.harness.cdng.manifest.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.data.Outcome;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManifestListConfig implements Outcome {
  private List<ManifestConfigWrapper> manifests;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ManifestListConfig(@JsonProperty("manifests") List<ManifestConfigWrapper> manifests) {
    this.manifests = manifests;
  }
}
