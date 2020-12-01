package io.harness.pms.cdng.service;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.pms.cdng.artifact.bean.yaml.ArtifactListConfigPms;
import io.harness.pms.cdng.artifact.bean.yaml.ArtifactOverrideSetsPms;
import io.harness.pms.cdng.manifest.yaml.ManifestConfigWrapperPms;
import io.harness.pms.cdng.manifest.yaml.ManifestOverrideSetsPms;
import io.harness.yaml.core.intfc.WithType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface ServiceSpecPms extends WithType {
  ArtifactListConfigPms getArtifacts();
  List<ManifestConfigWrapperPms> getManifests();
  List<ManifestOverrideSetsPms> getManifestOverrideSets();
  List<ArtifactOverrideSetsPms> getArtifactOverrideSets();
  @Override @JsonIgnore String getType();
}
