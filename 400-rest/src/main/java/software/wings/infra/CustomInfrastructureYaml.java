package software.wings.infra;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.InfrastructureType;
import software.wings.beans.NameValuePair;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(InfrastructureType.CUSTOM_INFRASTRUCTURE)
public final class CustomInfrastructureYaml extends CloudProviderInfrastructureYaml {
  private List<NameValuePair> infraVariables;
  private String deploymentTypeTemplateVersion;

  @Builder
  public CustomInfrastructureYaml(
      String type, List<NameValuePair> infraVariables, String deploymentTypeTemplateVersion) {
    super(type);
    setInfraVariables(infraVariables);
    setDeploymentTypeTemplateVersion(deploymentTypeTemplateVersion);
  }

  public CustomInfrastructureYaml() {
    super(InfrastructureType.CUSTOM_INFRASTRUCTURE);
  }
}
