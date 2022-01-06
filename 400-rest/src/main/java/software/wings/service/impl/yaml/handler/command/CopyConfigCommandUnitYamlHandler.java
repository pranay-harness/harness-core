/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.command;

import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_DESTINATION_PARENT_PATH;

import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.CopyConfigCommandUnit;
import software.wings.beans.command.CopyConfigCommandUnit.Yaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
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

  @Override
  protected CopyConfigCommandUnit toBean(ChangeContext<CopyConfigCommandUnit.Yaml> changeContext) {
    CopyConfigCommandUnit copyConfigCommandUnit = super.toBean(changeContext);
    Yaml yaml = changeContext.getYaml();
    copyConfigCommandUnit.setDestinationParentPath(yaml.getDestinationParentPath());
    return copyConfigCommandUnit;
  }

  @Override
  public CopyConfigCommandUnit toBean(AbstractCommandUnit.Yaml yaml) {
    CopyConfigCommandUnit.Yaml copyConfigYaml = (CopyConfigCommandUnit.Yaml) yaml;
    CopyConfigCommandUnit copyConfigCommandUnit = super.toBean(yaml);
    copyConfigCommandUnit.setDestinationParentPath(copyConfigYaml.getDestinationParentPath());
    return copyConfigCommandUnit;
  }

  @Override
  protected Map<String, Object> getNodeProperties(ChangeContext<Yaml> changeContext) {
    Map<String, Object> nodeProperties = super.getNodeProperties(changeContext);
    nodeProperties.put(NODE_PROPERTY_DESTINATION_PARENT_PATH, "$WINGS_RUNTIME_PATH");
    return nodeProperties;
  }
}
