package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.AbstractCommandUnitYaml;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.command.ScpCommandUnitYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.utils.Utils;

import com.google.inject.Singleton;
import java.util.Map;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class ScpCommandUnitYamlHandler extends SshCommandUnitYamlHandler<ScpCommandUnitYaml, ScpCommandUnit> {
  @Override
  public Class getYamlClass() {
    return ScpCommandUnitYaml.class;
  }

  @Override
  protected ScpCommandUnit getCommandUnit() {
    return new ScpCommandUnit();
  }

  @Override
  public ScpCommandUnitYaml toYaml(ScpCommandUnit bean, String appId) {
    ScpCommandUnitYaml yaml = ScpCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setDestinationDirectoryPath(bean.getDestinationDirectoryPath());
    String fileCategory = Utils.getStringFromEnum(bean.getFileCategory());
    yaml.setSource(fileCategory);
    yaml.setArtifactVariableName(bean.getArtifactVariableName());
    return yaml;
  }

  @Override
  protected ScpCommandUnit toBean(ChangeContext<ScpCommandUnitYaml> changeContext) {
    ScpCommandUnit scpCommandUnit = super.toBean(changeContext);
    ScpCommandUnitYaml yaml = changeContext.getYaml();
    scpCommandUnit.setDestinationDirectoryPath(yaml.getDestinationDirectoryPath());
    ScpFileCategory scpFileCategory = Utils.getEnumFromString(ScpFileCategory.class, yaml.getSource());
    scpCommandUnit.setFileCategory(scpFileCategory);
    scpCommandUnit.setArtifactVariableName(yaml.getArtifactVariableName());
    return scpCommandUnit;
  }

  @Override
  public ScpCommandUnit toBean(AbstractCommandUnitYaml yaml) {
    ScpCommandUnitYaml scpYaml = (ScpCommandUnitYaml) yaml;
    ScpCommandUnit scpCommandUnit = super.toBean(yaml);
    scpCommandUnit.setDestinationDirectoryPath(scpYaml.getDestinationDirectoryPath());
    ScpFileCategory scpFileCategory = Utils.getEnumFromString(ScpFileCategory.class, scpYaml.getSource());
    scpCommandUnit.setFileCategory(scpFileCategory);
    scpCommandUnit.setArtifactVariableName(scpYaml.getArtifactVariableName());
    return scpCommandUnit;
  }

  @Override
  protected Map<String, Object> getNodeProperties(ChangeContext<ScpCommandUnitYaml> changeContext) {
    Map<String, Object> nodeProperties = super.getNodeProperties(changeContext);
    ScpCommandUnitYaml yaml = changeContext.getYaml();
    nodeProperties.put(YamlConstants.NODE_PROPERTY_FILE_CATEGORY, yaml.getSource());
    nodeProperties.put(YamlConstants.NODE_PROPERTY_DESTINATION_DIR_PATH, yaml.getDestinationDirectoryPath());
    nodeProperties.put(YamlConstants.NODE_PROPERTY_ARTIFACT_VARIABLE_NAME, yaml.getArtifactVariableName());
    return nodeProperties;
  }
}
