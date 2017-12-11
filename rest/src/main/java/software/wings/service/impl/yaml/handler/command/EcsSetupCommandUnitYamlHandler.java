package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.EcsSetupCommandUnit;
import software.wings.beans.command.EcsSetupCommandUnit.Yaml;
import software.wings.beans.command.EcsSetupCommandUnit.Yaml.Builder;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author brett on 11/28/17
 */
public class EcsSetupCommandUnitYamlHandler
    extends ContainerSetupCommandUnitYamlHandler<Yaml, EcsSetupCommandUnit, Builder> {
  @Override
  public EcsSetupCommandUnit upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
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
  protected EcsSetupCommandUnit getCommandUnit() {
    return new EcsSetupCommandUnit();
  }
}
