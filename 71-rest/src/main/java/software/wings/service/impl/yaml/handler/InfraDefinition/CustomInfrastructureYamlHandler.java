package software.wings.service.impl.yaml.handler.InfraDefinition;

import software.wings.beans.InfrastructureType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.CustomInfrastructure;
import software.wings.infra.CustomInfrastructure.Yaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;

import java.util.List;

public class CustomInfrastructureYamlHandler
    extends CloudProviderInfrastructureYamlHandler<Yaml, CustomInfrastructure> {
  @Override
  public Yaml toYaml(CustomInfrastructure bean, String appId) {
    return Yaml.builder()
        .type(InfrastructureType.CUSTOM_INFRASTRUCTURE)
        .infraVariables(bean.getInfraVariables())
        .build();
  }

  @Override
  public CustomInfrastructure upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml yaml = changeContext.getYaml();
    return CustomInfrastructure.builder().infraVariables(yaml.getInfraVariables()).build();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
