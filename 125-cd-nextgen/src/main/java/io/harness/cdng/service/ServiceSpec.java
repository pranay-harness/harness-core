package io.harness.cdng.service;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets;
import io.harness.yaml.core.intfc.WithType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface ServiceSpec extends WithType {
  ArtifactListConfig getArtifacts();
  List<ManifestConfigWrapper> getManifests();
  List<ManifestOverrideSets> getManifestOverrideSets();
  List<ArtifactOverrideSets> getArtifactOverrideSets();
  @Override @JsonIgnore String getType();
}
