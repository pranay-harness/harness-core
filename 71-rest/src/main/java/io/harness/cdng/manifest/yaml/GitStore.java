package io.harness.cdng.manifest.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.data.structure.EmptyPredicate;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.experimental.Wither;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestStoreType.GIT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitStore implements StoreConfig {
  @Wither private String connectorId;
  @Wither @Singular private List<String> paths;
  @Wither private FetchType fetchType;
  @Wither private String fetchValue;
  @Builder.Default private String kind = ManifestStoreType.GIT;

  public GitStore cloneInternal() {
    return GitStore.builder().connectorId(connectorId).fetchType(fetchType).fetchValue(fetchValue).paths(paths).build();
  }

  @Override
  public StoreConfig applyOverrides(StoreConfig overrideConfig) {
    GitStore gitStore = (GitStore) overrideConfig;
    GitStore resultantGitStore = this;
    if (EmptyPredicate.isNotEmpty(gitStore.getConnectorId())) {
      resultantGitStore = resultantGitStore.withConnectorId(gitStore.getConnectorId());
    }
    if (EmptyPredicate.isNotEmpty(gitStore.getPaths())) {
      resultantGitStore = resultantGitStore.withPaths(gitStore.getPaths());
    }
    if (gitStore.getFetchType() != null) {
      resultantGitStore = resultantGitStore.withFetchType(gitStore.getFetchType());
    }
    if (EmptyPredicate.isNotEmpty(gitStore.getFetchValue())) {
      resultantGitStore = resultantGitStore.withFetchValue(gitStore.getFetchValue());
    }
    return resultantGitStore;
  }
}
