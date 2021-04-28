package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.common.SwaggerConstants;
import io.harness.filters.ConnectorRefExtractorHelper;
import io.harness.filters.WithConnectorRef;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestStoreType.GCS)
@SimpleVisitorHelper(helperClass = ConnectorRefExtractorHelper.class)
@TypeAlias("gcsStore")
public class GcsStoreConfig implements StoreConfig, Visitable, WithConnectorRef {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> connectorRef;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> bucketName;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> folderPath;

  @Override
  public String getKind() {
    return ManifestStoreType.GCS;
  }

  @Override
  public StoreConfig cloneInternal() {
    return GcsStoreConfig.builder().connectorRef(connectorRef).bucketName(bucketName).folderPath(folderPath).build();
  }

  @Override
  public StoreConfig applyOverrides(StoreConfig overrideConfig) {
    GcsStoreConfig gcsStoreConfig = (GcsStoreConfig) overrideConfig;
    GcsStoreConfig resultantGcsStore = this;
    if (!ParameterField.isNull(gcsStoreConfig.getConnectorRef())) {
      resultantGcsStore = resultantGcsStore.withConnectorRef(gcsStoreConfig.getConnectorRef());
    }

    if (!ParameterField.isNull(gcsStoreConfig.getBucketName())) {
      resultantGcsStore = resultantGcsStore.withBucketName(gcsStoreConfig.getBucketName());
    }

    if (!ParameterField.isNull(gcsStoreConfig.getFolderPath())) {
      resultantGcsStore = resultantGcsStore.withFolderPath(gcsStoreConfig.getFolderPath());
    }

    return resultantGcsStore;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName("spec").isPartOfFQN(false).build();
  }

  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
    connectorRefMap.put(YAMLFieldNameConstants.CONNECTOR_REF, connectorRef);
    return connectorRefMap;
  }
}
