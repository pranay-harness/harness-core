package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Singleton;

import software.wings.beans.command.KubernetesSetupCommandUnit;
import software.wings.beans.command.KubernetesSetupCommandUnit.Yaml;

/**
 * @author brett on 11/28/17
 */
@Singleton
public class KubernetesSetupCommandUnitYamlHandler
    extends ContainerSetupCommandUnitYamlHandler<Yaml, KubernetesSetupCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(KubernetesSetupCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected KubernetesSetupCommandUnit getCommandUnit() {
    return new KubernetesSetupCommandUnit();
  }
}
