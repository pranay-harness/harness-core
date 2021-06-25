package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.common.ParameterFieldHelper;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.filters.ConnectorRefExtractorHelper;
import io.harness.filters.WithConnectorRef;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.validation.OneOfField;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.YamlSchemaTypes;

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
@JsonTypeName(ManifestStoreType.GITHUB)
@OneOfField(fields = {"paths", "folderPath"})
@OneOfField(fields = {"branch", "commitId"})
@SimpleVisitorHelper(helperClass = ConnectorRefExtractorHelper.class)
@TypeAlias("githubStore")
@OwnedBy(CDP)
public class GithubStore implements GitStoreConfig, Visitable, WithConnectorRef {
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
  @Wither
  private ParameterField<String> connectorRef;

  @NotNull @Wither private FetchType gitFetchType;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> branch;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> commitId;

  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  @Wither
  private ParameterField<List<String>> paths;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> folderPath;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> repoName;

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
        .folderPath(folderPath)
        .repoName(repoName)
        .build();
  }

  @Override
  public io.harness.cdng.manifest.yaml.storeConfig.StoreConfig applyOverrides(StoreConfig overrideConfig) {
    GithubStore githubStore = (GithubStore) overrideConfig;
    GithubStore resultantGithubStore = this;
    if (!ParameterField.isNull(githubStore.getConnectorRef())) {
      resultantGithubStore = resultantGithubStore.withConnectorRef(githubStore.getConnectorRef());
    }
    if (!ParameterField.isNull(githubStore.getPaths())) {
      resultantGithubStore = resultantGithubStore.withPaths(githubStore.getPaths());
    }
    if (!ParameterField.isNull(githubStore.getFolderPath())) {
      resultantGithubStore = resultantGithubStore.withFolderPath(githubStore.getFolderPath());
    }
    if (githubStore.getGitFetchType() != null) {
      resultantGithubStore = resultantGithubStore.withGitFetchType(githubStore.getGitFetchType());
    }
    if (!ParameterField.isNull(githubStore.getBranch())) {
      resultantGithubStore = resultantGithubStore.withBranch(githubStore.getBranch()).withCommitId(null);
    }
    if (!ParameterField.isNull(githubStore.getCommitId())) {
      resultantGithubStore = resultantGithubStore.withCommitId(githubStore.getCommitId()).withBranch(null);
    }
    if (!ParameterField.isNull(githubStore.getRepoName())) {
      resultantGithubStore = resultantGithubStore.withRepoName(githubStore.getRepoName());
    }
    return resultantGithubStore;
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
    return GithubStoreDTO.builder()
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
