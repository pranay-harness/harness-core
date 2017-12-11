package software.wings.service.impl.yaml.handler.variable;

import software.wings.beans.ErrorCode;
import software.wings.beans.Variable;
import software.wings.beans.Variable.Yaml;
import software.wings.beans.VariableType;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.utils.Util;

import java.util.List;

/**
 * @author rktummala on 10/28/17
 */
public class VariableYamlHandler extends BaseYamlHandler<Variable.Yaml, Variable> {
  @Override
  public Variable createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  private Variable setWithYamlValues(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    VariableType variableType = Util.getEnumFromString(VariableType.class, yaml.getType());
    return Variable.VariableBuilder.aVariable()
        .withDescription(yaml.getDescription())
        .withFixed(yaml.isFixed())
        .withMandatory(yaml.isMandatory())
        .withName(yaml.getName())
        .withType(variableType)
        .withValue(yaml.getValue())
        .build();
  }

  @Override
  public Yaml toYaml(Variable bean, String appId) {
    return Yaml.Builder.anYaml()
        .withDescription(bean.getDescription())
        .withFixed(bean.isFixed())
        .withMandatory(bean.isMandatory())
        .withName(bean.getName())
        .withType(bean.getType().name())
        .withValue(bean.getValue())
        .build();
  }

  @Override
  public Variable upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }

  @Override
  public Variable updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return Variable.Yaml.class;
  }

  @Override
  public Variable get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
