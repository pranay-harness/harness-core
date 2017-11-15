package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.KubernetesResizeCommandUnit;
import software.wings.beans.command.KubernetesResizeCommandUnit.Yaml;
import software.wings.beans.command.KubernetesResizeCommandUnit.Yaml.Builder;

/**
 * @author rktummala on 11/13/17
 */
public class KubernetesResizeCommandUnitYamlHandler
    extends ContainerCommandUnitYamlHandler<Yaml, KubernetesResizeCommandUnit, Builder> {
  @Override
  public Class getYamlClass() {
    return KubernetesResizeCommandUnit.Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.aYaml();
  }

  @Override
  protected KubernetesResizeCommandUnit getCommandUnit() {
    return new KubernetesResizeCommandUnit();
  }
}
