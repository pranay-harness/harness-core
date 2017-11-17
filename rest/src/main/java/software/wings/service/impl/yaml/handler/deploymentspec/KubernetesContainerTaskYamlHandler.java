package software.wings.service.impl.yaml.handler.deploymentspec;

import software.wings.api.DeploymentType;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.KubernetesContainerTask.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
public class KubernetesContainerTaskYamlHandler extends ContainerTaskYamlHandler<Yaml, KubernetesContainerTask> {
  @Override
  protected Yaml getContainerTaskYaml() {
    return new Yaml();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public KubernetesContainerTask get(String accountId, String yamlFilePath) {
    return getContainerTask(accountId, yamlFilePath, DeploymentType.KUBERNETES.name());
  }

  @Override
  public KubernetesContainerTask updateFromYaml(ChangeContext<KubernetesContainerTask.Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    KubernetesContainerTask previous = get(accountId, changeContext.getChange().getFilePath());
    KubernetesContainerTask containerTask = setWithYamlValues(previous, changeContext, changeSetContext);
    return (KubernetesContainerTask) serviceResourceService.updateContainerTask(containerTask, false);
  }
}
