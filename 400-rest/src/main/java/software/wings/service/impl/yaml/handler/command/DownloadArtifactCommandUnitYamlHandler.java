package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.AbstractCommandUnitYaml;
import software.wings.beans.command.DownloadArtifactCommandUnit;
import software.wings.beans.command.DownloadArtifactCommandUnitYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;

import java.util.Map;

public class DownloadArtifactCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<DownloadArtifactCommandUnitYaml, DownloadArtifactCommandUnit> {
  @Override
  public Class getYamlClass() {
    return DownloadArtifactCommandUnitYaml.class;
  }

  @Override
  public DownloadArtifactCommandUnitYaml toYaml(DownloadArtifactCommandUnit bean, String appId) {
    DownloadArtifactCommandUnitYaml yaml = DownloadArtifactCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactVariableName(bean.getArtifactVariableName());
    return yaml;
  }

  @Override
  protected DownloadArtifactCommandUnit getCommandUnit() {
    return new DownloadArtifactCommandUnit();
  }

  @Override
  protected Map<String, Object> getNodeProperties(ChangeContext<DownloadArtifactCommandUnitYaml> changeContext) {
    Map<String, Object> nodeProperties = super.getNodeProperties(changeContext);
    DownloadArtifactCommandUnitYaml yaml = changeContext.getYaml();
    nodeProperties.put(YamlConstants.NODE_PROPERTY_ARTIFACT_VARIABLE_NAME, yaml.getArtifactVariableName());
    return nodeProperties;
  }

  @Override
  protected DownloadArtifactCommandUnit toBean(ChangeContext<DownloadArtifactCommandUnitYaml> changeContext) {
    DownloadArtifactCommandUnitYaml yaml = changeContext.getYaml();
    DownloadArtifactCommandUnit downloadArtifactCommandUnit = super.toBean(changeContext);
    downloadArtifactCommandUnit.setArtifactVariableName(yaml.getArtifactVariableName());
    return downloadArtifactCommandUnit;
  }

  @Override
  public DownloadArtifactCommandUnit toBean(AbstractCommandUnitYaml yaml) {
    DownloadArtifactCommandUnitYaml downloadYaml = (DownloadArtifactCommandUnitYaml) yaml;
    DownloadArtifactCommandUnit downloadArtifactCommandUnit = super.toBean(yaml);
    downloadArtifactCommandUnit.setArtifactVariableName(downloadYaml.getArtifactVariableName());
    return downloadArtifactCommandUnit;
  }
}
