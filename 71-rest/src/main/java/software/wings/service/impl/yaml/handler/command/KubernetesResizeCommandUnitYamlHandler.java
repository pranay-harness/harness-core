package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Singleton;

import software.wings.beans.command.KubernetesResizeCommandUnit;
import software.wings.beans.command.KubernetesResizeCommandUnit.Yaml;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class KubernetesResizeCommandUnitYamlHandler
    extends ContainerResizeCommandUnitYamlHandler<Yaml, KubernetesResizeCommandUnit> {
  @Override
  public Class getYamlClass() {
    return KubernetesResizeCommandUnit.Yaml.class;
  }

  @Override
  public Yaml toYaml(KubernetesResizeCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected KubernetesResizeCommandUnit getCommandUnit() {
    return new KubernetesResizeCommandUnit();
  }
}
