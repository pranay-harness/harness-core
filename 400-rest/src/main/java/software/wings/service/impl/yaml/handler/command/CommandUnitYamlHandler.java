/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.command;

import static software.wings.beans.Graph.graphIdGenerator;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.GraphNode;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  @author rktummala on 11/13/17
 */
public abstract class CommandUnitYamlHandler<Y extends AbstractCommandUnit.Yaml, C extends CommandUnit>
    extends BaseYamlHandler<Y, C> {
  protected abstract C getCommandUnit();

  public GraphNode getGraphNode(ChangeContext<Y> changeContext, GraphNode previousNode) {
    Y yaml = changeContext.getYaml();
    return GraphNode.builder()
        .name(yaml.getName())
        .type(yaml.getCommandUnitType())
        .properties(getNodeProperties(changeContext))
        .origin(previousNode == null)
        .id(getNodeId())
        .valid(false)
        .build();
    //    .templateExpressions().rollback(yaml.get);
  }

  private String getNodeId() {
    return graphIdGenerator(YamlConstants.NODE_PREFIX);
  }

  protected Map<String, Object> getNodeProperties(ChangeContext<Y> changeContext) {
    return new HashMap<>();
  }

  protected C toBean(ChangeContext<Y> changeContext) {
    Y yaml = changeContext.getYaml();
    CommandUnitType commandUnitType = Utils.getEnumFromString(CommandUnitType.class, yaml.getCommandUnitType());
    C commandUnit = getCommandUnit();
    commandUnit.setDeploymentType(yaml.getDeploymentType());
    commandUnit.setCommandUnitType(commandUnitType);
    commandUnit.setName(yaml.getName());
    return commandUnit;
  }

  public C toBean(AbstractCommandUnit.Yaml yaml) {
    C commandUnit = getCommandUnit();
    commandUnit.setDeploymentType(yaml.getDeploymentType());
    commandUnit.setCommandUnitType(Utils.getEnumFromString(CommandUnitType.class, yaml.getCommandUnitType()));
    commandUnit.setName(yaml.getName());
    return commandUnit;
  }

  protected void toYaml(Y yaml, C bean) {
    String commandUnitType = Utils.getStringFromEnum(bean.getCommandUnitType());
    yaml.setCommandUnitType(commandUnitType);
    yaml.setDeploymentType(bean.getDeploymentType());
    yaml.setName(bean.getName());
  }

  @Override
  public C get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Y> changeContext) {
    // do nothing
  }

  @Override
  public C upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext) {
    return toBean(changeContext);
  }
}
