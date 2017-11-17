package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.ErrorCode;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.utils.Util;

import java.util.List;

/**
 *  @author rktummala on 11/13/17
 */
public abstract class CommandUnitYamlHandler<Y extends AbstractCommandUnit.Yaml, C extends CommandUnit,
                                             B extends AbstractCommandUnit.Yaml.Builder> extends BaseYamlHandler<Y, C> {
  protected abstract B getYamlBuilder();
  protected abstract C getCommandUnit();

  @Override
  public C createFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  private C setWithYamlValues(ChangeContext<Y> changeContext) throws HarnessException {
    Y yaml = changeContext.getYaml();
    CommandUnitType commandUnitType = Util.getEnumFromString(CommandUnitType.class, yaml.getCommandUnitType());
    C commandUnit = getCommandUnit();
    commandUnit.setDeploymentType(yaml.getDeploymentType());
    commandUnit.setCommandUnitType(commandUnitType);
    commandUnit.setName(yaml.getName());
    return commandUnit;
  }

  @Override
  public Y toYaml(C bean, String appId) {
    String commandUnitType = Util.getStringFromEnum(bean.getCommandUnitType());
    return getYamlBuilder()
        .withCommandUnitType(commandUnitType)
        .withDeploymentType(bean.getDeploymentType())
        .withName(bean.getName())
        .build();
  }

  @Override
  public C updateFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  @Override
  public boolean validate(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext) {
    Y yaml = changeContext.getYaml();
    return !(yaml == null || yaml.getCommandUnitType() == null || yaml.getName() == null
        || yaml.getDeploymentType() == null);
  }

  @Override
  public C get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }
}
