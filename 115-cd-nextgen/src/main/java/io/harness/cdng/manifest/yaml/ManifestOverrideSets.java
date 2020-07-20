package io.harness.cdng.manifest.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.yaml.core.intfc.OverrideSetsWrapper;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("overrideSet")
public class ManifestOverrideSets implements OverrideSetsWrapper {
  String identifier;
  List<ManifestConfigWrapper> manifests;
}
