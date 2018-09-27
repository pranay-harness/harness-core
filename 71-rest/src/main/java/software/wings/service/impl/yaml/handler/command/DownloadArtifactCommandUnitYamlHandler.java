package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.DownloadArtifactCommandUnit;
import software.wings.beans.command.DownloadArtifactCommandUnit.Yaml;

public class DownloadArtifactCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<DownloadArtifactCommandUnit.Yaml, DownloadArtifactCommandUnit> {
  @Override
  public Class getYamlClass() {
    return DownloadArtifactCommandUnit.Yaml.class;
  }

  @Override
  public DownloadArtifactCommandUnit.Yaml toYaml(DownloadArtifactCommandUnit bean, String appId) {
    DownloadArtifactCommandUnit.Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected DownloadArtifactCommandUnit getCommandUnit() {
    return new DownloadArtifactCommandUnit();
  }
}
