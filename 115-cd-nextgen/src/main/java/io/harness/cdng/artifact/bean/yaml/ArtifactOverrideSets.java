package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.yaml.core.intfc.OverrideSetsWrapper;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("overrideSet")
public class ArtifactOverrideSets implements OverrideSetsWrapper {
  String identifier;
  ArtifactListConfig artifacts;
}
