package software.wings.service.impl.yaml.handler.command;

import static software.wings.beans.yaml.YamlConstants.COORDINATE_INCREMENT_BY;
import static software.wings.beans.yaml.YamlConstants.DEFAULT_COORDINATE;

import com.google.common.collect.Maps;

import software.wings.beans.ErrorCode;
import software.wings.beans.Graph.Node;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.common.UUIDGenerator;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.utils.Util;

import java.util.List;
import java.util.Map;

/**
 *  @author rktummala on 11/13/17
 */
public abstract class CommandUnitYamlHandler<Y extends AbstractCommandUnit.Yaml, C extends CommandUnit,
                                             B extends AbstractCommandUnit.Yaml.Builder> extends BaseYamlHandler<Y, C> {
  protected abstract B getYamlBuilder();
  protected abstract C getCommandUnit();

  protected Node getGraphNode(ChangeContext<Y> changeContext, Node previousNode) {
    Y yaml = changeContext.getYaml();
    int xCoordinate = previousNode == null ? DEFAULT_COORDINATE : previousNode.getX() + COORDINATE_INCREMENT_BY;
    return Node.Builder.aNode()
        .withName(yaml.getName())
        .withType(yaml.getCommandUnitType())
        .withProperties(getNodeProperties(changeContext))
        .withOrigin(previousNode == null)
        .withExpanded(false)
        .withId(getNodeId())
        .withValid(false)
        .withHeight(0)
        .withWidth(0)
        .withX(xCoordinate)
        .withY(DEFAULT_COORDINATE)
        .build();
    //    .withTemplateExpressions().withRollback(yaml.get);
  }

  private String getNodeId() {
    return UUIDGenerator.graphIdGenerator(YamlConstants.NODE_PREFIX);
  }

  protected Map<String, Object> getNodeProperties(ChangeContext<Y> changeContext) {
    return Maps.newHashMap();
  }

  @Override
  public C createFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  private C setWithYamlValues(ChangeContext<Y> changeContext) throws HarnessException {
    Y yaml = changeContext.getYaml();
    CommandUnitType commandUnitType = Util.getEnumFromString(CommandUnitType.class, yaml.getCommandUnitType());
    C commandUnit = getCommandUnit();
    commandUnit.setDeploymentType(yaml.getDeploymentType());
    commandUnit.setCommandUnitType(commandUnitType);
    commandUnit.setName(yaml.getName());
    return commandUnit;
  }

  @Override
  public Y toYaml(C bean, String appId) {
    String commandUnitType = Util.getStringFromEnum(bean.getCommandUnitType());
    return getYamlBuilder()
        .withCommandUnitType(commandUnitType)
        .withDeploymentType(bean.getDeploymentType())
        .withName(bean.getName())
        .build();
  }

  @Override
  public C updateFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  @Override
  public boolean validate(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext) {
    Y yaml = changeContext.getYaml();
    return !(yaml == null || yaml.getCommandUnitType() == null || yaml.getName() == null
        || yaml.getDeploymentType() == null);
  }

  @Override
  public C get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }
}
