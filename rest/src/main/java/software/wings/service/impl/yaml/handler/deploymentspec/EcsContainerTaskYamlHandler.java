package software.wings.service.impl.yaml.handler.deploymentspec;

import software.wings.api.DeploymentType;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsContainerTask.Yaml;

/**
 * @author rktummala on 11/15/17
 */
public class EcsContainerTaskYamlHandler extends ContainerTaskYamlHandler<Yaml, EcsContainerTask> {
  @Override
  public Yaml toYaml(EcsContainerTask bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public EcsContainerTask get(String accountId, String yamlFilePath) {
    return getContainerTask(accountId, yamlFilePath, DeploymentType.ECS.name());
  }
}
