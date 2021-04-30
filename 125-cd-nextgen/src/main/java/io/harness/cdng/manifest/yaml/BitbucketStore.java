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
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestStoreType.BITBUCKET)
@OneOfField(fields = {"paths", "folderPath"})
@OneOfField(fields = {"branch", "commitId"})
@SimpleVisitorHelper(helperClass = ConnectorRefExtractorHelper.class)
@TypeAlias("bitbucketStore")
@OwnedBy(CDP)
public class BitbucketStore implements GitStoreConfig, Visitable, WithConnectorRef {
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
    return ManifestStoreType.BITBUCKET;
  }

  public BitbucketStore cloneInternal() {
    return BitbucketStore.builder()
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
    BitbucketStore bitbucketStore = (BitbucketStore) overrideConfig;
    BitbucketStore resultantBitbucketStore = this;
    if (!ParameterField.isNull(bitbucketStore.getConnectorRef())) {
      resultantBitbucketStore = resultantBitbucketStore.withConnectorRef(bitbucketStore.getConnectorRef());
    }
    if (!ParameterField.isNull(bitbucketStore.getPaths())) {
      resultantBitbucketStore = resultantBitbucketStore.withPaths(bitbucketStore.getPaths());
    }
    if (!ParameterField.isNull(bitbucketStore.getFolderPath())) {
      resultantBitbucketStore = resultantBitbucketStore.withFolderPath(bitbucketStore.getFolderPath());
    }
    if (bitbucketStore.getGitFetchType() != null) {
      resultantBitbucketStore = resultantBitbucketStore.withGitFetchType(bitbucketStore.getGitFetchType());
    }
    if (!ParameterField.isNull(bitbucketStore.getBranch())) {
      resultantBitbucketStore = resultantBitbucketStore.withBranch(bitbucketStore.getBranch());
    }
    if (!ParameterField.isNull(bitbucketStore.getCommitId())) {
      resultantBitbucketStore = resultantBitbucketStore.withCommitId(bitbucketStore.getCommitId());
    }
    if (!ParameterField.isNull(bitbucketStore.getRepoName())) {
      resultantBitbucketStore = resultantBitbucketStore.withRepoName(bitbucketStore.getRepoName());
    }

    return resultantBitbucketStore;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    return VisitableChildren.builder().build();
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName("spec").isPartOfFQN(false).build();
  }

  @Override
  public GitStoreConfigDTO toGitStoreConfigDTO() {
    return BitBucketStoreDTO.builder()
        .branch(ParameterFieldHelper.getParameterFieldValue(branch))
        .commitId(ParameterFieldHelper.getParameterFieldValue(commitId))
        .connectorRef(ParameterFieldHelper.getParameterFieldValue(connectorRef))
        .folderPath(ParameterFieldHelper.getParameterFieldValue(folderPath))
        .gitFetchType(gitFetchType)
        .paths(ParameterFieldHelper.getParameterFieldValue(paths))
        .repoName(ParameterFieldHelper.getParameterFieldValue(repoName))
        .build();
  }

  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
    connectorRefMap.put(YAMLFieldNameConstants.CONNECTOR_REF, connectorRef);
    return connectorRefMap;
  }
}