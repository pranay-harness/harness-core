package io.harness.cdng.manifest.yaml;

import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.visitor.helper.HttpStoreVisitorHelper;
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
@JsonTypeName(ManifestStoreType.HTTP)
@SimpleVisitorHelper(helperClass = HttpStoreVisitorHelper.class)
@TypeAlias("httpStore")
public class HttpStoreConfig implements StoreConfig {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> connectorRef;

  @Override
  public String getKind() {
    return ManifestStoreType.HTTP;
  }

  @Override
  public StoreConfig cloneInternal() {
    return HttpStoreConfig.builder().connectorRef(connectorRef).build();
  }

  @Override
  public StoreConfig applyOverrides(StoreConfig overrideConfig) {
    HttpStoreConfig helmHttpStore = (HttpStoreConfig) overrideConfig;
    HttpStoreConfig resultantHelmHttpStore = this;
    if (!ParameterField.isNull(helmHttpStore.getConnectorRef())) {
      resultantHelmHttpStore = resultantHelmHttpStore.withConnectorRef(helmHttpStore.getConnectorRef());
    }

    return resultantHelmHttpStore;
  }
}
