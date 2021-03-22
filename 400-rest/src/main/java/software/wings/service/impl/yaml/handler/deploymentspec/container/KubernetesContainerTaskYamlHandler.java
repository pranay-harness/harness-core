package software.wings.service.impl.yaml.handler.deploymentspec.container;

import software.wings.api.DeploymentType;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.KubernetesContainerTaskYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class KubernetesContainerTaskYamlHandler
    extends ContainerTaskYamlHandler<KubernetesContainerTaskYaml, KubernetesContainerTask> {
  @Override
  public KubernetesContainerTaskYaml toYaml(KubernetesContainerTask bean, String appId) {
    KubernetesContainerTaskYaml yaml = KubernetesContainerTaskYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return KubernetesContainerTaskYaml.class;
  }

  @Override
  public KubernetesContainerTask get(String accountId, String yamlFilePath) {
    return getContainerTask(accountId, yamlFilePath, DeploymentType.KUBERNETES.name());
  }

  @Override
  protected KubernetesContainerTask createNewContainerTask() {
    return new KubernetesContainerTask();
  }
}
