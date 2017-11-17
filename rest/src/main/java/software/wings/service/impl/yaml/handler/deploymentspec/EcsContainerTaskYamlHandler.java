package software.wings.service.impl.yaml.handler.deploymentspec;

import software.wings.api.DeploymentType;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsContainerTask.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
public class EcsContainerTaskYamlHandler extends ContainerTaskYamlHandler<EcsContainerTask.Yaml, EcsContainerTask> {
  @Override
  protected Yaml getContainerTaskYaml() {
    return new EcsContainerTask.Yaml();
  }

  @Override
  public Class getYamlClass() {
    return EcsContainerTask.Yaml.class;
  }

  @Override
  public EcsContainerTask get(String accountId, String yamlFilePath) {
    return getContainerTask(accountId, yamlFilePath, DeploymentType.ECS.name());
  }

  @Override
  public EcsContainerTask updateFromYaml(ChangeContext<EcsContainerTask.Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    EcsContainerTask previous = get(accountId, changeContext.getChange().getFilePath());
    EcsContainerTask containerTask = setWithYamlValues(previous, changeContext, changeSetContext);
    return (EcsContainerTask) serviceResourceService.updateContainerTask(containerTask, false);
  }
}
