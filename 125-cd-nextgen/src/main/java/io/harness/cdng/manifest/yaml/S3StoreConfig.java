package io.harness.cdng.manifest.yaml;

import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.visitor.helper.S3StoreVisitorHelper;
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
@JsonTypeName(ManifestStoreType.S3)
@SimpleVisitorHelper(helperClass = S3StoreVisitorHelper.class)
@TypeAlias("s3Store")
public class S3StoreConfig implements StoreConfig {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> connectorRef;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> bucketName;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> region;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> folderPath;

  @Override
  public String getKind() {
    return ManifestStoreType.S3;
  }

  @Override
  public StoreConfig cloneInternal() {
    return S3StoreConfig.builder()
        .connectorRef(connectorRef)
        .bucketName(bucketName)
        .region(region)
        .folderPath(folderPath)
        .build();
  }

  @Override
  public StoreConfig applyOverrides(StoreConfig overrideConfig) {
    S3StoreConfig s3StoreConfig = (S3StoreConfig) overrideConfig;
    S3StoreConfig resultantS3Store = this;
    if (!ParameterField.isNull(s3StoreConfig.getConnectorRef())) {
      resultantS3Store = resultantS3Store.withConnectorRef(s3StoreConfig.getConnectorRef());
    }

    if (!ParameterField.isNull(s3StoreConfig.getBucketName())) {
      resultantS3Store = resultantS3Store.withBucketName(s3StoreConfig.getBucketName());
    }

    if (!ParameterField.isNull(s3StoreConfig.getRegion())) {
      resultantS3Store = resultantS3Store.withRegion(s3StoreConfig.getRegion());
    }

    if (!ParameterField.isNull(s3StoreConfig.getFolderPath())) {
      resultantS3Store = resultantS3Store.withFolderPath(s3StoreConfig.getFolderPath());
    }

    return resultantS3Store;
  }
}
