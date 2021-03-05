package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.KubernetesSetupCommandUnit;
import software.wings.beans.command.KubernetesSetupCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author brett on 11/28/17
 */
@Singleton
public class KubernetesSetupCommandUnitYamlHandler
    extends ContainerSetupCommandUnitYamlHandler<KubernetesSetupCommandUnitYaml, KubernetesSetupCommandUnit> {
  @Override
  public Class getYamlClass() {
    return KubernetesSetupCommandUnitYaml.class;
  }

  @Override
  public KubernetesSetupCommandUnitYaml toYaml(KubernetesSetupCommandUnit bean, String appId) {
    KubernetesSetupCommandUnitYaml yaml = KubernetesSetupCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected KubernetesSetupCommandUnit getCommandUnit() {
    return new KubernetesSetupCommandUnit();
  }
}
