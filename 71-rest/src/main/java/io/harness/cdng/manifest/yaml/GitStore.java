package io.harness.cdng.manifest.yaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.visitor.helper.GitStoreVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.walktree.visitor.SimpleVisitorHelper;
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
@SimpleVisitorHelper(helperClass = GitStoreVisitorHelper.class)
public class GitStore implements StoreConfig {
  @Wither private String connectorIdentifier;
  @Wither private FetchType gitFetchType;
  @Wither private String branch;
  @Wither private String commitId;
  @Wither @Singular private List<String> paths;

  @Override
  public String getKind() {
    return ManifestStoreType.GIT;
  }

  public GitStore cloneInternal() {
    return GitStore.builder()
        .connectorIdentifier(connectorIdentifier)
        .gitFetchType(gitFetchType)
        .branch(branch)
        .commitId(commitId)
        .paths(paths)
        .build();
  }

  @Override
  public StoreConfig applyOverrides(StoreConfig overrideConfig) {
    GitStore gitStore = (GitStore) overrideConfig;
    GitStore resultantGitStore = this;
    if (EmptyPredicate.isNotEmpty(gitStore.getConnectorIdentifier())) {
      resultantGitStore = resultantGitStore.withConnectorIdentifier(gitStore.getConnectorIdentifier());
    }
    if (EmptyPredicate.isNotEmpty(gitStore.getPaths())) {
      resultantGitStore = resultantGitStore.withPaths(gitStore.getPaths());
    }
    if (gitStore.getGitFetchType() != null) {
      resultantGitStore = resultantGitStore.withGitFetchType(gitStore.getGitFetchType());
    }
    if (EmptyPredicate.isNotEmpty(gitStore.getBranch())) {
      resultantGitStore = resultantGitStore.withBranch(gitStore.getBranch());
    }
    if (EmptyPredicate.isNotEmpty(gitStore.getCommitId())) {
      resultantGitStore = resultantGitStore.withCommitId(gitStore.getCommitId());
    }
    return resultantGitStore;
  }
}
