package io.harness.cdng.manifest.yaml;

import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.visitor.helper.GcsStoreVisitorHelper;
import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.visitor.SimpleVisitorHelper;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestStoreType.GCS)
@SimpleVisitorHelper(helperClass = GcsStoreVisitorHelper.class)
@TypeAlias("gcsStore")
public class GcsStoreConfig implements StoreConfig {
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
}
