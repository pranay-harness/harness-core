package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.KubernetesSetupCommandUnit.Yaml;
import software.wings.beans.command.KubernetesSetupCommandUnit.Yaml.Builder;
import software.wings.beans.command.KubernetesSetupCommandUnit;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author brett on 11/28/17
 */
public class KubernetesSetupCommandUnitYamlHandler
    extends ContainerSetupCommandUnitYamlHandler<Yaml, KubernetesSetupCommandUnit, Builder> {
  @Override
  public KubernetesSetupCommandUnit upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.aYaml();
  }

  @Override
  protected KubernetesSetupCommandUnit getCommandUnit() {
    return new KubernetesSetupCommandUnit();
  }
}
