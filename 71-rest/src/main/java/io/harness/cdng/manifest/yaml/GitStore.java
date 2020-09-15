package io.harness.cdng.manifest.yaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.ParameterField;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.visitor.helper.GitStoreVisitorHelper;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestStoreType.GIT)
@SimpleVisitorHelper(helperClass = GitStoreVisitorHelper.class)
public class GitStore implements StoreConfig, Visitable {
  @Wither private ParameterField<String> connectorIdentifier;
  @Wither private FetchType gitFetchType;
  @Wither private ParameterField<String> branch;
  @Wither private ParameterField<String> commitId;
  @Wither private ParameterField<List<String>> paths;

  // For Visitor Framework Impl
  String metadata;

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
    if (gitStore.getConnectorIdentifier() != null) {
      resultantGitStore = resultantGitStore.withConnectorIdentifier(gitStore.getConnectorIdentifier());
    }
    if (gitStore.getPaths() != null) {
      resultantGitStore = resultantGitStore.withPaths(gitStore.getPaths());
    }
    if (gitStore.getGitFetchType() != null) {
      resultantGitStore = resultantGitStore.withGitFetchType(gitStore.getGitFetchType());
    }
    if (gitStore.getBranch() != null) {
      resultantGitStore = resultantGitStore.withBranch(gitStore.getBranch());
    }
    if (gitStore.getCommitId() != null) {
      resultantGitStore = resultantGitStore.withCommitId(gitStore.getCommitId());
    }
    return resultantGitStore;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    return VisitableChildren.builder().build();
  }
}
