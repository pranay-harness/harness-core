package software.wings.service.impl.yaml.handler.workflow;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import software.wings.beans.ErrorCode;
import software.wings.beans.Graph.Node;
import software.wings.beans.NameValuePair;
import software.wings.beans.ObjectType;
import software.wings.beans.TemplateExpression;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.utils.Util;
import software.wings.yaml.workflow.StepYaml;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/28/17
 */
public class StepYamlHandler extends BaseYamlHandler<StepYaml, Node> {
  @Inject YamlHandlerFactory yamlHandlerFactory;

  @Override
  public Node createFromYaml(ChangeContext<StepYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(true, changeContext, changeSetContext);
  }

  private Node setWithYamlValues(boolean isCreate, ChangeContext<StepYaml> changeContext,
      List<ChangeContext> changeContextList) throws HarnessException {
    StepYaml yaml = changeContext.getYaml();

    // template expressions
    List<TemplateExpression> templateExpressions = Lists.newArrayList();
    if (yaml.getTemplateExpressions() != null) {
      BaseYamlHandler templateExprYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION, ObjectType.TEMPLATE_EXPRESSION);
      templateExpressions = yaml.getTemplateExpressions()
                                .stream()
                                .map(templateExpr -> {
                                  try {
                                    ChangeContext.Builder clonedContext =
                                        cloneFileChangeContext(changeContext, templateExpr);
                                    return (TemplateExpression) templateExprYamlHandler.createFromYaml(
                                        clonedContext.build(), changeContextList);
                                  } catch (HarnessException e) {
                                    throw new WingsException(e);
                                  }
                                })
                                .collect(Collectors.toList());
    }

    // properties
    Map<String, Object> properties = Maps.newHashMap();
    if (yaml.getProperties() != null) {
      List<NameValuePair> nameValuePairList =
          yaml.getProperties()
              .stream()
              .map(nvpYaml -> NameValuePair.builder().name(nvpYaml.getName()).value(nvpYaml.getValue()).build())
              .collect(Collectors.toList());
      properties = Util.toProperties(nameValuePairList);
    }

    return Node.Builder.aNode()
        .withName(yaml.getName())
        .withType(yaml.getType())
        .withRollback(yaml.isRollback())
        .withTemplateExpressions(templateExpressions)
        .withProperties(properties)
        .build();
  }

  @Override
  public StepYaml toYaml(Node bean, String appId) {
    List<NameValuePair.Yaml> nameValuePairYamlList = Lists.newArrayList();
    if (bean.getProperties() != null) {
      List<NameValuePair> nameValuePairs = Util.toYamlList(bean.getProperties());
      // properties
      BaseYamlHandler nameValuePairYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.NAME_VALUE_PAIR, ObjectType.NAME_VALUE_PAIR);
      nameValuePairYamlList =
          nameValuePairs.stream()
              .map(nameValuePair -> (NameValuePair.Yaml) nameValuePairYamlHandler.toYaml(nameValuePair, appId))
              .collect(Collectors.toList());
    }

    // template expressions
    List<TemplateExpression.Yaml> templateExprYamlList = Lists.newArrayList();
    if (bean.getTemplateExpressions() != null) {
      BaseYamlHandler templateExpressionYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION, ObjectType.TEMPLATE_EXPRESSION);
      templateExprYamlList =
          bean.getTemplateExpressions()
              .stream()
              .map(templateExpression
                  -> (TemplateExpression.Yaml) templateExpressionYamlHandler.toYaml(templateExpression, appId))
              .collect(Collectors.toList());
    }

    return StepYaml.Builder.aYaml()
        .withName(bean.getName())
        .withProperties(nameValuePairYamlList)
        .withRollback(bean.getRollback())
        .withType(bean.getType())
        .withTemplateExpressions(templateExprYamlList)
        .build();
  }

  @Override
  public Node upsertFromYaml(ChangeContext<StepYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }

  @Override
  public Node updateFromYaml(ChangeContext<StepYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(false, changeContext, changeSetContext);
  }

  @Override
  public boolean validate(ChangeContext<StepYaml> changeContext, List<ChangeContext> changeSetContext) {
    return false;
  }

  @Override
  public Class getYamlClass() {
    return StepYaml.class;
  }

  @Override
  public Node get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<StepYaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
