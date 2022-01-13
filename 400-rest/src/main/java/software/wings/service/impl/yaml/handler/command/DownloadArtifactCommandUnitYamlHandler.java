/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.DownloadArtifactCommandUnit;
import software.wings.beans.command.DownloadArtifactCommandUnit.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;

import java.util.Map;

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
    yaml.setArtifactVariableName(bean.getArtifactVariableName());
    return yaml;
  }

  @Override
  protected DownloadArtifactCommandUnit getCommandUnit() {
    return new DownloadArtifactCommandUnit();
  }

  @Override
  protected Map<String, Object> getNodeProperties(ChangeContext<Yaml> changeContext) {
    Map<String, Object> nodeProperties = super.getNodeProperties(changeContext);
    DownloadArtifactCommandUnit.Yaml yaml = changeContext.getYaml();
    nodeProperties.put(YamlConstants.NODE_PROPERTY_ARTIFACT_VARIABLE_NAME, yaml.getArtifactVariableName());
    return nodeProperties;
  }

  @Override
  protected DownloadArtifactCommandUnit toBean(ChangeContext<DownloadArtifactCommandUnit.Yaml> changeContext) {
    DownloadArtifactCommandUnit.Yaml yaml = changeContext.getYaml();
    DownloadArtifactCommandUnit downloadArtifactCommandUnit = super.toBean(changeContext);
    downloadArtifactCommandUnit.setArtifactVariableName(yaml.getArtifactVariableName());
    return downloadArtifactCommandUnit;
  }

  @Override
  public DownloadArtifactCommandUnit toBean(AbstractCommandUnit.Yaml yaml) {
    DownloadArtifactCommandUnit.Yaml downloadYaml = (DownloadArtifactCommandUnit.Yaml) yaml;
    DownloadArtifactCommandUnit downloadArtifactCommandUnit = super.toBean(yaml);
    downloadArtifactCommandUnit.setArtifactVariableName(downloadYaml.getArtifactVariableName());
    return downloadArtifactCommandUnit;
  }
}
