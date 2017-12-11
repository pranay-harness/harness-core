package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.KubernetesResizeCommandUnit;
import software.wings.beans.command.KubernetesResizeCommandUnit.Yaml;
import software.wings.beans.command.KubernetesResizeCommandUnit.Yaml.Builder;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/13/17
 */
public class KubernetesResizeCommandUnitYamlHandler
    extends ContainerResizeCommandUnitYamlHandler<Yaml, KubernetesResizeCommandUnit, Builder> {
  @Override
  public KubernetesResizeCommandUnit upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }

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
