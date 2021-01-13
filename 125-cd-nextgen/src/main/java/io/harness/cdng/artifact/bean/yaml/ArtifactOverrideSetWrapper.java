package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("artifactOverrideSetsWrapper")
public class ArtifactOverrideSetWrapper {
  ArtifactOverrideSets overrideSet;
  String uuid;
}
