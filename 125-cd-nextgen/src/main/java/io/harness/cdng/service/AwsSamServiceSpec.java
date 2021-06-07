package io.harness.cdng.service;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSetWrapper;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSetWrapper;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.cdng.service.beans.ServiceSpecType;
import io.harness.cdng.variables.beans.NGVariableOverrideSetWrapper;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName(ServiceSpecType.AWS_SAM)
public class AwsSamServiceSpec implements ServiceSpec {
  List<NGVariable> variables;
  ArtifactListConfig artifacts;
  List<ManifestConfigWrapper> manifests;

  List<NGVariableOverrideSetWrapper> variableOverrideSets;
  List<ArtifactOverrideSetWrapper> artifactOverrideSets;
  List<ManifestOverrideSetWrapper> manifestOverrideSets;

  @Override
  public String getType() {
    return ServiceDefinitionType.AWS_SAM.getYamlName();
  }
}
