package io.harness.cdng.manifest.yaml;

import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.visitor.helper.BitbucketStoreVisitorHelper;
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
@JsonTypeName(ManifestStoreType.BITBUCKET)
@SimpleVisitorHelper(helperClass = BitbucketStoreVisitorHelper.class)
@TypeAlias("bitbucketStore")
public class BitbucketStore implements GitStoreConfig, Visitable {
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
    return ManifestStoreType.BITBUCKET;
  }

  public BitbucketStore cloneInternal() {
    return BitbucketStore.builder()
        .connectorRef(connectorRef)
        .gitFetchType(gitFetchType)
        .branch(branch)
        .commitId(commitId)
        .paths(paths)
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
    if (bitbucketStore.getGitFetchType() != null) {
      resultantBitbucketStore = resultantBitbucketStore.withGitFetchType(bitbucketStore.getGitFetchType());
    }
    if (!ParameterField.isNull(bitbucketStore.getBranch())) {
      resultantBitbucketStore = resultantBitbucketStore.withBranch(bitbucketStore.getBranch());
    }
    if (!ParameterField.isNull(bitbucketStore.getCommitId())) {
      resultantBitbucketStore = resultantBitbucketStore.withCommitId(bitbucketStore.getCommitId());
    }
    return resultantBitbucketStore;
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