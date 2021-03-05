package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.KubernetesResizeCommandUnit;
import software.wings.beans.command.KubernetesResizeCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class KubernetesResizeCommandUnitYamlHandler
    extends ContainerResizeCommandUnitYamlHandler<KubernetesResizeCommandUnitYaml, KubernetesResizeCommandUnit> {
  @Override
  public Class getYamlClass() {
    return KubernetesResizeCommandUnitYaml.class;
  }

  @Override
  public KubernetesResizeCommandUnitYaml toYaml(KubernetesResizeCommandUnit bean, String appId) {
    KubernetesResizeCommandUnitYaml yaml = KubernetesResizeCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected KubernetesResizeCommandUnit getCommandUnit() {
    return new KubernetesResizeCommandUnit();
  }
}
