package io.harness.cdng.service.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets;
import io.harness.cdng.service.ServiceSpec;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonTypeName(ServiceDefinitionType.KUBERNETES)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesServiceSpec implements ServiceSpec {
  ArtifactListConfig artifacts;
  List<ManifestConfigWrapper> manifests;
  List<ManifestOverrideSets> manifestOverrideSets;
  List<ArtifactOverrideSets> artifactOverrideSets;

  @Override
  public String getType() {
    return ServiceDefinitionType.KUBERNETES;
  }
}
