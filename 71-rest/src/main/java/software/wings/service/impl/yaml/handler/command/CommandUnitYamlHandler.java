package software.wings.service.impl.yaml.handler.command;

import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;

import com.google.common.collect.Maps;

import io.harness.data.structure.UUIDGenerator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.GraphNode;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.utils.Util;

import java.util.List;
import java.util.Map;

/**
 *  @author rktummala on 11/13/17
 */
public abstract class CommandUnitYamlHandler<Y extends AbstractCommandUnit.Yaml, C extends CommandUnit>
    extends BaseYamlHandler<Y, C> {
  protected abstract C getCommandUnit();

  protected GraphNode getGraphNode(ChangeContext<Y> changeContext, GraphNode previousNode) {
    Y yaml = changeContext.getYaml();
    return aGraphNode()
        .withName(yaml.getName())
        .withType(yaml.getCommandUnitType())
        .withProperties(getNodeProperties(changeContext))
        .withOrigin(previousNode == null)
        .withId(getNodeId())
        .withValid(false)
        .build();
    //    .withTemplateExpressions().withRollback(yaml.get);
  }

  private String getNodeId() {
    return UUIDGenerator.graphIdGenerator(YamlConstants.NODE_PREFIX);
  }

  protected Map<String, Object> getNodeProperties(ChangeContext<Y> changeContext) {
    return Maps.newHashMap();
  }

  protected C toBean(ChangeContext<Y> changeContext) throws HarnessException {
    Y yaml = changeContext.getYaml();
    CommandUnitType commandUnitType = Util.getEnumFromString(CommandUnitType.class, yaml.getCommandUnitType());
    C commandUnit = getCommandUnit();
    commandUnit.setDeploymentType(yaml.getDeploymentType());
    commandUnit.setCommandUnitType(commandUnitType);
    commandUnit.setName(yaml.getName());
    return commandUnit;
  }

  public C toBean(AbstractCommandUnit.Yaml yaml) {
    C commandUnit = getCommandUnit();
    commandUnit.setDeploymentType(yaml.getDeploymentType());
    commandUnit.setCommandUnitType(Util.getEnumFromString(CommandUnitType.class, yaml.getCommandUnitType()));
    commandUnit.setName(yaml.getName());
    return commandUnit;
  }

  protected void toYaml(Y yaml, C bean) {
    String commandUnitType = Util.getStringFromEnum(bean.getCommandUnitType());
    yaml.setCommandUnitType(commandUnitType);
    yaml.setDeploymentType(bean.getDeploymentType());
    yaml.setName(bean.getName());
  }

  @Override
  public C get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Y> changeContext) throws HarnessException {
    // do nothing
  }

  @Override
  public C upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext);
  }
}
