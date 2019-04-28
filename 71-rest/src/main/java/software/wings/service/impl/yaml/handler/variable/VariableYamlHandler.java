package software.wings.service.impl.yaml.handler.variable;

import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;
import software.wings.beans.Variable;
import software.wings.beans.Variable.VariableBuilder;
import software.wings.beans.Variable.Yaml;
import software.wings.beans.VariableType;
import software.wings.beans.yaml.ChangeContext;
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
        .description(yaml.getDescription())
        .fixed(yaml.isFixed())
        .mandatory(yaml.isMandatory())
        .name(yaml.getName())
        .type(variableType)
        .value(yaml.getValue())
        .allowedValues(yaml.getAllowedValues())
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
        .allowedValues(bean.getAllowedValues())
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
