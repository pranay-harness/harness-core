package io.harness.cdng.infra.yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.K8sGcpInfraMapping;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.pipelineinfrastructure.K8sGcpInfrastructureVisitorHelper;
import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName(InfrastructureKind.KUBERNETES_GCP)
@SimpleVisitorHelper(helperClass = K8sGcpInfrastructureVisitorHelper.class)
@TypeAlias("k8sGcpInfrastructure")
@OwnedBy(HarnessTeam.CDP)
public class K8sGcpInfrastructure implements Infrastructure, Visitable {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> connectorRef;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> namespace;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> releaseName;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> cluster;

  @Override
  public InfraMapping getInfraMapping() {
    return K8sGcpInfraMapping.builder()
        .gcpConnector(connectorRef.getValue())
        .namespace(namespace.getValue())
        .cluster(cluster.getValue())
        .build();
  }

  @Override
  public String getKind() {
    return InfrastructureKind.KUBERNETES_GCP;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.SPEC).isPartOfFQN(false).build();
  }

  @Override
  public Infrastructure applyOverrides(Infrastructure overrideConfig) {
    K8sGcpInfrastructure config = (K8sGcpInfrastructure) overrideConfig;
    K8sGcpInfrastructure resultantInfra = this;
    if (!ParameterField.isNull(config.getConnectorRef())) {
      resultantInfra = resultantInfra.withConnectorRef(config.getConnectorRef());
    }
    if (!ParameterField.isNull(config.getNamespace())) {
      resultantInfra = resultantInfra.withNamespace(config.getNamespace());
    }
    if (!ParameterField.isNull(config.getCluster())) {
      resultantInfra = resultantInfra.withCluster(config.getCluster());
    }
    if (!ParameterField.isNull(config.getReleaseName())) {
      resultantInfra = resultantInfra.withReleaseName(config.getReleaseName());
    }
    return resultantInfra;
  }
}
