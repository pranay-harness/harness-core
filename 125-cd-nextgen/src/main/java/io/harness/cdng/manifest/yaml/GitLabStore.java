package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.filters.ConnectorRefExtractorHelper;
import io.harness.filters.WithConnectorRef;
import io.harness.ngpipeline.common.ParameterFieldHelper;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.validation.OneOfField;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
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
@OneOfField(fields = {"branch", "commitId"})
@SimpleVisitorHelper(helperClass = ConnectorRefExtractorHelper.class)
@TypeAlias("gitLabStore")
@OwnedBy(CDP)
public class GitLabStore implements GitStoreConfig, Visitable, WithConnectorRef {
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
  @Wither
  private ParameterField<String> connectorRef;

  @NotNull @Wither private FetchType gitFetchType;
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
      resultantGitLabStore = resultantGitLabStore.withBranch(gitLabStore.getBranch()).withCommitId(null);
    }
    if (!ParameterField.isNull(gitLabStore.getCommitId())) {
      resultantGitLabStore = resultantGitLabStore.withCommitId(gitLabStore.getCommitId()).withBranch(null);
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
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
    connectorRefMap.put(YAMLFieldNameConstants.CONNECTOR_REF, connectorRef);
    return connectorRefMap;
  }

  @Override
  public GitStoreConfigDTO toGitStoreConfigDTO() {
    return GitLabStoreDTO.builder()
        .branch(ParameterFieldHelper.getParameterFieldValue(branch))
        .commitId(ParameterFieldHelper.getParameterFieldValue(commitId))
        .connectorRef(ParameterFieldHelper.getParameterFieldValue(connectorRef))
        .folderPath(ParameterFieldHelper.getParameterFieldValue(folderPath))
        .gitFetchType(gitFetchType)
        .paths(ParameterFieldHelper.getParameterFieldValue(paths))
        .repoName(ParameterFieldHelper.getParameterFieldValue(repoName))
        .build();
  }
}
