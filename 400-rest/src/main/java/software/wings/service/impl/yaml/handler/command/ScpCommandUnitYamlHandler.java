/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.command.ScpCommandUnit.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.utils.Utils;

import com.google.inject.Singleton;
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
    String fileCategory = Utils.getStringFromEnum(bean.getFileCategory());
    yaml.setSource(fileCategory);
    yaml.setArtifactVariableName(bean.getArtifactVariableName());
    return yaml;
  }

  @Override
  protected ScpCommandUnit toBean(ChangeContext<Yaml> changeContext) {
    ScpCommandUnit scpCommandUnit = super.toBean(changeContext);
    Yaml yaml = changeContext.getYaml();
    scpCommandUnit.setDestinationDirectoryPath(yaml.getDestinationDirectoryPath());
    ScpFileCategory scpFileCategory = Utils.getEnumFromString(ScpFileCategory.class, yaml.getSource());
    scpCommandUnit.setFileCategory(scpFileCategory);
    scpCommandUnit.setArtifactVariableName(yaml.getArtifactVariableName());
    return scpCommandUnit;
  }

  @Override
  public ScpCommandUnit toBean(AbstractCommandUnit.Yaml yaml) {
    ScpCommandUnit.Yaml scpYaml = (ScpCommandUnit.Yaml) yaml;
    ScpCommandUnit scpCommandUnit = super.toBean(yaml);
    scpCommandUnit.setDestinationDirectoryPath(scpYaml.getDestinationDirectoryPath());
    ScpFileCategory scpFileCategory = Utils.getEnumFromString(ScpFileCategory.class, scpYaml.getSource());
    scpCommandUnit.setFileCategory(scpFileCategory);
    scpCommandUnit.setArtifactVariableName(scpYaml.getArtifactVariableName());
    return scpCommandUnit;
  }

  @Override
  protected Map<String, Object> getNodeProperties(ChangeContext<Yaml> changeContext) {
    Map<String, Object> nodeProperties = super.getNodeProperties(changeContext);
    Yaml yaml = changeContext.getYaml();
    nodeProperties.put(YamlConstants.NODE_PROPERTY_FILE_CATEGORY, yaml.getSource());
    nodeProperties.put(YamlConstants.NODE_PROPERTY_DESTINATION_DIR_PATH, yaml.getDestinationDirectoryPath());
    nodeProperties.put(YamlConstants.NODE_PROPERTY_ARTIFACT_VARIABLE_NAME, yaml.getArtifactVariableName());
    return nodeProperties;
  }
}
