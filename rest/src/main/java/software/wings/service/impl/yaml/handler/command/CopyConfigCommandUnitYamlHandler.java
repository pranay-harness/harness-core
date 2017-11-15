package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.CopyConfigCommandUnit.Yaml.Builder;
import software.wings.beans.command.CopyConfigCommandUnit;
import software.wings.beans.command.CopyConfigCommandUnit.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/13/17
 */
public class CopyConfigCommandUnitYamlHandler
    extends CommandUnitYamlHandler<CopyConfigCommandUnit.Yaml, CopyConfigCommandUnit, Builder> {
  @Override
  public Class getYamlClass() {
    return CopyConfigCommandUnit.Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.aYaml();
  }

  @Override
  protected CopyConfigCommandUnit getCommandUnit() {
    return new CopyConfigCommandUnit();
  }

  @Override
  public CopyConfigCommandUnit createFromYaml(ChangeContext<CopyConfigCommandUnit.Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    CopyConfigCommandUnit copyConfigCommandUnit = super.createFromYaml(changeContext, changeSetContext);
    return setWithYamlValues(changeContext, copyConfigCommandUnit);
  }

  @Override
  public CopyConfigCommandUnit.Yaml toYaml(CopyConfigCommandUnit bean, String appId) {
    Yaml yaml = super.toYaml(bean, appId);
    yaml.setDestinationParentPath(bean.getDestinationParentPath());
    return yaml;
  }

  @Override
  public CopyConfigCommandUnit updateFromYaml(ChangeContext<CopyConfigCommandUnit.Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    CopyConfigCommandUnit copyConfigCommandUnit = super.updateFromYaml(changeContext, changeSetContext);
    return setWithYamlValues(changeContext, copyConfigCommandUnit);
  }

  private CopyConfigCommandUnit setWithYamlValues(
      ChangeContext<CopyConfigCommandUnit.Yaml> changeContext, CopyConfigCommandUnit copyConfigCommandUnit) {
    Yaml yaml = changeContext.getYaml();
    copyConfigCommandUnit.setDestinationParentPath(yaml.getDestinationParentPath());
    return copyConfigCommandUnit;
  }

  @Override
  public boolean validate(
      ChangeContext<CopyConfigCommandUnit.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    boolean validate = super.validate(changeContext, changeSetContext);
    CopyConfigCommandUnit.Yaml yaml = changeContext.getYaml();
    if (validate) {
      return !(yaml.getDestinationParentPath() == null);
    }

    return validate;
  }
}
