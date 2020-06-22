package io.harness.cdng.manifest.yaml.kinds;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.StoreConfig;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestType.VALUES)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValuesManifest implements ManifestAttributes {
  private String identifier;
  @JsonIgnore private StoreConfig storeConfig;
  @Builder.Default private String kind = ManifestType.VALUES;

  @JsonProperty(ManifestStoreType.GIT)
  public void setGitStore(GitStore gitStore) {
    gitStore.setKind(ManifestStoreType.GIT);
    this.storeConfig = gitStore;
  }
}
