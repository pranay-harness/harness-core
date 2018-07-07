package software.wings.service.impl.yaml.handler.template;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.ErrorCode;
import software.wings.beans.NameValuePair;
import software.wings.beans.TemplateExpression;
import software.wings.beans.TemplateExpression.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.NameValuePairYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.utils.Util;

import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 10/28/17
 */
@Singleton
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
              .collect(toList());
      properties = Util.toProperties(nameValuePairList);
    }

    return TemplateExpression.builder()
        .expression(yaml.getExpression())
        .fieldName(yaml.getFieldName())
        .metadata(properties)
        .build();
  }

  @Override
  public Yaml toYaml(TemplateExpression bean, String appId) {
    NameValuePairYamlHandler nameValuePairYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.NAME_VALUE_PAIR);
    List<NameValuePair.Yaml> nameValuePairYamlList =
        Util.toNameValuePairYamlList(bean.getMetadata(), appId, nameValuePairYamlHandler);

    return Yaml.Builder.aYaml()
        .withExpression(bean.getExpression())
        .withFieldName(bean.getFieldName())
        .withMetadata(nameValuePairYamlList)
        .build();
  }

  @Override
  public TemplateExpression upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext);
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
