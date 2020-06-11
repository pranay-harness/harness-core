package io.harness.cdng.manifest.yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestType.K8Manifest)
@JsonIgnoreProperties(ignoreUnknown = true)
public class K8Manifest implements ManifestAttributes {
  private String identifier;
  @JsonIgnore private StoreConfig storeConfig;
  @Builder.Default private String kind = ManifestType.K8Manifest;

  @JsonProperty(ManifestStoreType.GIT)
  public void setGitStore(GitStore gitStore) {
    gitStore.setKind(ManifestStoreType.GIT);
    this.storeConfig = gitStore;
  }
}
