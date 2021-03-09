package io.harness.cdng.manifest.yaml;

import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.visitor.helper.GithubStoreVisitorHelper;
import io.harness.common.SwaggerConstants;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestStoreType.GITHUB)
@SimpleVisitorHelper(helperClass = GithubStoreVisitorHelper.class)
@TypeAlias("githubStore")
public class GithubStore implements GitStoreConfig, Visitable {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> connectorRef;

  @Wither private FetchType gitFetchType;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> branch;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> commitId;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  @Wither
  private ParameterField<List<String>> paths;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public String getKind() {
    return ManifestStoreType.GITHUB;
  }

  public GithubStore cloneInternal() {
    return GithubStore.builder()
        .connectorRef(connectorRef)
        .gitFetchType(gitFetchType)
        .branch(branch)
        .commitId(commitId)
        .paths(paths)
        .build();
  }

  @Override
  public StoreConfig applyOverrides(StoreConfig overrideConfig) {
    GithubStore githubStore = (GithubStore) overrideConfig;
    GithubStore resultantGithubStore = this;
    if (!ParameterField.isNull(githubStore.getConnectorRef())) {
      resultantGithubStore = resultantGithubStore.withConnectorRef(githubStore.getConnectorRef());
    }
    if (!ParameterField.isNull(githubStore.getPaths())) {
      resultantGithubStore = resultantGithubStore.withPaths(githubStore.getPaths());
    }
    if (githubStore.getGitFetchType() != null) {
      resultantGithubStore = resultantGithubStore.withGitFetchType(githubStore.getGitFetchType());
    }
    if (!ParameterField.isNull(githubStore.getBranch())) {
      resultantGithubStore = resultantGithubStore.withBranch(githubStore.getBranch());
    }
    if (!ParameterField.isNull(githubStore.getCommitId())) {
      resultantGithubStore = resultantGithubStore.withCommitId(githubStore.getCommitId());
    }
    return resultantGithubStore;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    return VisitableChildren.builder().build();
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName("spec").build();
  }
}
