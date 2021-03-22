package io.harness.cdng.manifest.yaml;

import io.harness.steps.shellScript.manifest.ManifestStoreType;
import io.harness.cdng.visitor.helper.GitLabStoreVisitorHelper;
import io.harness.common.SwaggerConstants;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.shellScript.manifest.yaml.GitStoreConfig;
import io.harness.validation.OneOfField;
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
@JsonTypeName(ManifestStoreType.GITLAB)
@OneOfField(fields = {"paths", "folderPath"})
@SimpleVisitorHelper(helperClass = GitLabStoreVisitorHelper.class)
@TypeAlias("gitLabStore")
public class GitLabStore implements GitStoreConfig, Visitable {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> connectorRef;

  @Wither private FetchType gitFetchType;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> branch;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> commitId;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  @Wither
  private ParameterField<List<String>> paths;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> folderPath;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> repoName;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public String getKind() {
    return ManifestStoreType.GITLAB;
  }

  public GitLabStore cloneInternal() {
    return GitLabStore.builder()
        .connectorRef(connectorRef)
        .gitFetchType(gitFetchType)
        .branch(branch)
        .commitId(commitId)
        .paths(paths)
        .folderPath(folderPath)
        .repoName(repoName)
        .build();
  }

  @Override
  public StoreConfig applyOverrides(StoreConfig overrideConfig) {
    GitLabStore gitLabStore = (GitLabStore) overrideConfig;
    GitLabStore resultantGitLabStore = this;
    if (!ParameterField.isNull(gitLabStore.getConnectorRef())) {
      resultantGitLabStore = resultantGitLabStore.withConnectorRef(gitLabStore.getConnectorRef());
    }
    if (!ParameterField.isNull(gitLabStore.getPaths())) {
      resultantGitLabStore = resultantGitLabStore.withPaths(gitLabStore.getPaths());
    }
    if (!ParameterField.isNull(gitLabStore.getFolderPath())) {
      resultantGitLabStore = resultantGitLabStore.withFolderPath(gitLabStore.getFolderPath());
    }
    if (gitLabStore.getGitFetchType() != null) {
      resultantGitLabStore = resultantGitLabStore.withGitFetchType(gitLabStore.getGitFetchType());
    }
    if (!ParameterField.isNull(gitLabStore.getBranch())) {
      resultantGitLabStore = resultantGitLabStore.withBranch(gitLabStore.getBranch());
    }
    if (!ParameterField.isNull(gitLabStore.getCommitId())) {
      resultantGitLabStore = resultantGitLabStore.withCommitId(gitLabStore.getCommitId());
    }
    if (!ParameterField.isNull(gitLabStore.getRepoName())) {
      resultantGitLabStore = resultantGitLabStore.withRepoName(gitLabStore.getRepoName());
    }
    return resultantGitLabStore;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    return VisitableChildren.builder().build();
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YAMLFieldNameConstants.SPEC).isPartOfFQN(false).build();
  }
}
