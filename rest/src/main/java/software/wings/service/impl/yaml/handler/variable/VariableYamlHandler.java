package software.wings.service.impl.yaml.handler.variable;

import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import software.wings.beans.Variable;
import software.wings.beans.Variable.VariableBuilder;
import software.wings.beans.Variable.Yaml;
import software.wings.beans.VariableType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.utils.Util;

import java.util.List;

/**
 * @author rktummala on 10/28/17
 */
@Singleton
public class VariableYamlHandler extends BaseYamlHandler<Variable.Yaml, Variable> {
  private Variable toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    VariableType variableType = Util.getEnumFromString(VariableType.class, yaml.getType());
    return VariableBuilder.aVariable()
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
    return Yaml.builder()
        .description(bean.getDescription())
        .fixed(bean.isFixed())
        .mandatory(bean.isMandatory())
        .name(bean.getName())
        .type(bean.getType().name())
        .value(bean.getValue())
        .build();
  }

  @Override
  public Variable upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext);
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
