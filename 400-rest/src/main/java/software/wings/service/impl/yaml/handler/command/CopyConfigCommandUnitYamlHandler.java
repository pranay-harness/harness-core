package software.wings.service.impl.yaml.handler.command;

import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_DESTINATION_PARENT_PATH;

import software.wings.beans.command.AbstractCommandUnitYaml;
import software.wings.beans.command.CopyConfigCommandUnit;
import software.wings.beans.command.CopyConfigCommandUnitYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.Map;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class CopyConfigCommandUnitYamlHandler
    extends CommandUnitYamlHandler<CopyConfigCommandUnitYaml, CopyConfigCommandUnit> {
  @Override
  public Class getYamlClass() {
    return CopyConfigCommandUnitYaml.class;
  }

  @Override
  protected CopyConfigCommandUnit getCommandUnit() {
    return new CopyConfigCommandUnit();
  }

  @Override
  public CopyConfigCommandUnitYaml toYaml(CopyConfigCommandUnit bean, String appId) {
    CopyConfigCommandUnitYaml yaml = CopyConfigCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setDestinationParentPath(bean.getDestinationParentPath());
    return yaml;
  }

  @Override
  protected CopyConfigCommandUnit toBean(ChangeContext<CopyConfigCommandUnitYaml> changeContext) {
    CopyConfigCommandUnit copyConfigCommandUnit = super.toBean(changeContext);
    CopyConfigCommandUnitYaml yaml = changeContext.getYaml();
    copyConfigCommandUnit.setDestinationParentPath(yaml.getDestinationParentPath());
    return copyConfigCommandUnit;
  }

  @Override
  public CopyConfigCommandUnit toBean(AbstractCommandUnitYaml yaml) {
    CopyConfigCommandUnitYaml copyConfigYaml = (CopyConfigCommandUnitYaml) yaml;
    CopyConfigCommandUnit copyConfigCommandUnit = super.toBean(yaml);
    copyConfigCommandUnit.setDestinationParentPath(copyConfigYaml.getDestinationParentPath());
    return copyConfigCommandUnit;
  }

  @Override
  protected Map<String, Object> getNodeProperties(ChangeContext<CopyConfigCommandUnitYaml> changeContext) {
    Map<String, Object> nodeProperties = super.getNodeProperties(changeContext);
    nodeProperties.put(NODE_PROPERTY_DESTINATION_PARENT_PATH, "$WINGS_RUNTIME_PATH");
    return nodeProperties;
  }
}
