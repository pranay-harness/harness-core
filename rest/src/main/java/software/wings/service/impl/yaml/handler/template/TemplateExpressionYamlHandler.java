package software.wings.service.impl.yaml.handler.template;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import software.wings.beans.ErrorCode;
import software.wings.beans.NameValuePair;
import software.wings.beans.ObjectType;
import software.wings.beans.TemplateExpression;
import software.wings.beans.TemplateExpression.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.utils.Util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/28/17
 */
public class TemplateExpressionYamlHandler extends BaseYamlHandler<TemplateExpression.Yaml, TemplateExpression> {
  @Inject YamlHandlerFactory yamlHandlerFactory;

  private TemplateExpression toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    Map<String, Object> properties = Maps.newHashMap();
    if (yaml.getMetadata() != null) {
      List<NameValuePair> nameValuePairList =
          yaml.getMetadata()
              .stream()
              .map(nvpYaml -> NameValuePair.builder().name(nvpYaml.getName()).value(nvpYaml.getValue()).build())
              .collect(Collectors.toList());
      properties = Util.toProperties(nameValuePairList);
    }

    return TemplateExpression.Builder.aTemplateExpression()
        .withDescription(yaml.getDescription())
        .withExpression(yaml.getExpression())
        .withExpressionAllowed(yaml.isExpressionAllowed())
        .withFieldName(yaml.getFieldName())
        .withMetadata(properties)
        .withMandatory(yaml.isMandatory())
        .build();
  }

  @Override
  public Yaml toYaml(TemplateExpression bean, String appId) {
    List<NameValuePair> nameValuePairs = Util.toYamlList(bean.getMetadata());
    // properties
    BaseYamlHandler nameValuePairYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.NAME_VALUE_PAIR, ObjectType.NAME_VALUE_PAIR);
    List<NameValuePair.Yaml> nameValuePairYamlList =
        nameValuePairs.stream()
            .map(nameValuePair -> (NameValuePair.Yaml) nameValuePairYamlHandler.toYaml(nameValuePair, appId))
            .collect(Collectors.toList());

    return Yaml.Builder.aYaml()
        .withDescription(bean.getDescription())
        .withExpression(bean.getExpression())
        .withExpressionAllowed(bean.isExpressionAllowed())
        .withFieldName(bean.getFieldName())
        .withMandatory(bean.isMandatory())
        .withMetadata(nameValuePairYamlList)
        .build();
  }

  @Override
  public TemplateExpression upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext);
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return TemplateExpression.Yaml.class;
  }

  @Override
  public TemplateExpression get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
