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
@JsonTypeName(ManifestStoreType.HTTP)
@SimpleVisitorHelper(helperClass = ConnectorRefExtractorHelper.class)
@TypeAlias("httpStore")
public class HttpStoreConfig implements StoreConfig, Visitable, WithConnectorRef {
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

  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
    connectorRefMap.put(YAMLFieldNameConstants.CONNECTOR_REF, connectorRef);
    return connectorRefMap;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName("spec").isPartOfFQN(false).build();
  }
}
