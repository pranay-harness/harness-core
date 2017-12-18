package software.wings.service.impl.yaml.handler.deploymentspec;

import software.wings.api.DeploymentType;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.KubernetesContainerTask.Yaml;

/**
 * @author rktummala on 11/15/17
 */
public class KubernetesContainerTaskYamlHandler extends ContainerTaskYamlHandler<Yaml, KubernetesContainerTask> {
  @Override
  public Yaml toYaml(KubernetesContainerTask bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public KubernetesContainerTask get(String accountId, String yamlFilePath) {
    return getContainerTask(accountId, yamlFilePath, DeploymentType.KUBERNETES.name());
  }
}
