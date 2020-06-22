package io.harness.cdng.manifest.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.manifest.ManifestStoreType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestStoreType.GIT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitStore implements StoreConfig {
  private String connectorId;
  @Singular private List<String> paths;
  private FetchType fetchType;
  private String fetchValue;
  @Builder.Default private String kind = ManifestStoreType.GIT;

  public GitStore cloneInternal() {
    return GitStore.builder().connectorId(connectorId).fetchType(fetchType).fetchValue(fetchValue).paths(paths).build();
  }
}
