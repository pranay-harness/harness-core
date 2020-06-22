package io.harness.cdng.manifest.yaml.kinds;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.ValuesPathProvider;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.StoreConfig;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestType.K8Manifest)
@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sManifest implements ManifestAttributes, ValuesPathProvider {
  private String identifier;
  @Singular private List<String> valuesFilePaths;
  @JsonIgnore private StoreConfig storeConfig;
  @Builder.Default private String kind = ManifestType.K8Manifest;

  @JsonProperty(ManifestStoreType.GIT)
  public void setGitStore(GitStore gitStore) {
    gitStore.setKind(ManifestStoreType.GIT);
    this.storeConfig = gitStore;
  }

  @Override
  public List<String> getValuesPathsToFetch() {
    return valuesFilePaths;
  }
}
