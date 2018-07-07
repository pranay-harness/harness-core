package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Singleton;

import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.command.ScpCommandUnit.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.exception.HarnessException;
import software.wings.utils.Util;

import java.util.Map;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class ScpCommandUnitYamlHandler extends SshCommandUnitYamlHandler<Yaml, ScpCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected ScpCommandUnit getCommandUnit() {
    return new ScpCommandUnit();
  }

  @Override
  public Yaml toYaml(ScpCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setDestinationDirectoryPath(bean.getDestinationDirectoryPath());
    String fileCategory = Util.getStringFromEnum(bean.getFileCategory());
    yaml.setSource(fileCategory);
    return yaml;
  }

  protected ScpCommandUnit toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    ScpCommandUnit scpCommandUnit = super.toBean(changeContext);
    Yaml yaml = changeContext.getYaml();
    scpCommandUnit.setDestinationDirectoryPath(yaml.getDestinationDirectoryPath());
    ScpFileCategory scpFileCategory = Util.getEnumFromString(ScpFileCategory.class, yaml.getSource());
    scpCommandUnit.setFileCategory(scpFileCategory);
    return scpCommandUnit;
  }

  @Override
  public ScpCommandUnit toBean(AbstractCommandUnit.Yaml yaml) {
    ScpCommandUnit.Yaml scpYaml = (ScpCommandUnit.Yaml) yaml;
    ScpCommandUnit scpCommandUnit = super.toBean(yaml);
    scpCommandUnit.setDestinationDirectoryPath(scpYaml.getDestinationDirectoryPath());
    ScpFileCategory scpFileCategory = Util.getEnumFromString(ScpFileCategory.class, scpYaml.getSource());
    scpCommandUnit.setFileCategory(scpFileCategory);
    return scpCommandUnit;
  }

  @Override
  protected Map<String, Object> getNodeProperties(ChangeContext<Yaml> changeContext) {
    Map<String, Object> nodeProperties = super.getNodeProperties(changeContext);
    Yaml yaml = changeContext.getYaml();
    nodeProperties.put(YamlConstants.NODE_PROPERTY_FILE_CATEGORY, yaml.getSource());
    nodeProperties.put(YamlConstants.NODE_PROPERTY_DESTINATION_DIR_PATH, yaml.getDestinationDirectoryPath());
    return nodeProperties;
  }
}
