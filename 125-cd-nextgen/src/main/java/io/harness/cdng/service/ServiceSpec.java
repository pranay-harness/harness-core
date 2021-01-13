package io.harness.cdng.service;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSetWrapper;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSetWrapper;
import io.harness.cdng.variables.beans.NGVariableOverrideSetWrapper;
import io.harness.yaml.core.intfc.WithType;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface ServiceSpec extends WithType {
  ArtifactListConfig getArtifacts();
  List<ManifestConfigWrapper> getManifests();
  List<ManifestOverrideSetWrapper> getManifestOverrideSets();
  List<ArtifactOverrideSetWrapper> getArtifactOverrideSets();
  List<NGVariableOverrideSetWrapper> getVariableOverrideSets();
  List<NGVariable> getVariables();
  @Override @JsonIgnore String getType();
}
