package software.wings.service.impl.yaml.handler.command;

import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_DESTINATION_PARENT_PATH;

import com.google.inject.Singleton;

import software.wings.beans.command.CopyConfigCommandUnit;
import software.wings.beans.command.CopyConfigCommandUnit.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class CopyConfigCommandUnitYamlHandler
    extends CommandUnitYamlHandler<CopyConfigCommandUnit.Yaml, CopyConfigCommandUnit> {
  @Override
  public Class getYamlClass() {
    return CopyConfigCommandUnit.Yaml.class;
  }

  @Override
  protected CopyConfigCommandUnit getCommandUnit() {
    return new CopyConfigCommandUnit();
  }

  @Override
  public CopyConfigCommandUnit.Yaml toYaml(CopyConfigCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setDestinationParentPath(bean.getDestinationParentPath());
    return yaml;
  }

  protected CopyConfigCommandUnit toBean(ChangeContext<CopyConfigCommandUnit.Yaml> changeContext)
      throws HarnessException {
    CopyConfigCommandUnit copyConfigCommandUnit = super.toBean(changeContext);
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

  @Override
  protected Map<String, Object> getNodeProperties(ChangeContext<Yaml> changeContext) {
    Map<String, Object> nodeProperties = super.getNodeProperties(changeContext);
    nodeProperties.put(NODE_PROPERTY_DESTINATION_PARENT_PATH, "$WINGS_RUNTIME_PATH");
    return nodeProperties;
  }
}
