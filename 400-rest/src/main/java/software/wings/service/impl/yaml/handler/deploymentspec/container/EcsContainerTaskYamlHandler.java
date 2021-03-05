package software.wings.service.impl.yaml.handler.deploymentspec.container;

import software.wings.api.DeploymentType;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsContainerTaskYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class EcsContainerTaskYamlHandler extends ContainerTaskYamlHandler<EcsContainerTaskYaml, EcsContainerTask> {
  @Override
  public EcsContainerTaskYaml toYaml(EcsContainerTask bean, String appId) {
    EcsContainerTaskYaml yaml = EcsContainerTaskYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return EcsContainerTaskYaml.class;
  }

  @Override
  public EcsContainerTask get(String accountId, String yamlFilePath) {
    return getContainerTask(accountId, yamlFilePath, DeploymentType.ECS.name());
  }

  @Override
  protected EcsContainerTask createNewContainerTask() {
    return new EcsContainerTask();
  }
}
