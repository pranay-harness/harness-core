package software.wings.service.impl.yaml.handler.InfraDefinition;

import software.wings.beans.InfrastructureType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.CustomInfrastructure;
import software.wings.infra.CustomInfrastructureYaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;

import java.util.List;

public class CustomInfrastructureYamlHandler
    extends CloudProviderInfrastructureYamlHandler<CustomInfrastructureYaml, CustomInfrastructure> {
  @Override
  public CustomInfrastructureYaml toYaml(CustomInfrastructure bean, String appId) {
    return CustomInfrastructureYaml.builder()
        .type(InfrastructureType.CUSTOM_INFRASTRUCTURE)
        .infraVariables(bean.getInfraVariables())
        .deploymentTypeTemplateVersion(bean.getDeploymentTypeTemplateVersion())
        .build();
  }

  @Override
  public CustomInfrastructure upsertFromYaml(
      ChangeContext<CustomInfrastructureYaml> changeContext, List<ChangeContext> changeSetContext) {
    CustomInfrastructureYaml yaml = changeContext.getYaml();
    return CustomInfrastructure.builder()
        .infraVariables(yaml.getInfraVariables())
        .deploymentTypeTemplateVersion(yaml.getDeploymentTypeTemplateVersion())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return CustomInfrastructureYaml.class;
  }
}
