package software.wings.service.impl.yaml.handler;

import software.wings.beans.ErrorCode;
import software.wings.beans.NameValuePair;
import software.wings.beans.NameValuePair.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;

import java.util.List;

/**
 * @author rktummala on 10/28/17
 */
public class NameValuePairYamlHandler extends BaseYamlHandler<NameValuePair.Yaml, NameValuePair> {
  @Override
  public NameValuePair createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  private NameValuePair setWithYamlValues(ChangeContext<Yaml> changeContext) throws HarnessException {
    NameValuePair.Yaml yaml = changeContext.getYaml();
    return NameValuePair.builder().name(yaml.getName()).value(yaml.getValue()).build();
  }

  @Override
  public NameValuePair.Yaml toYaml(NameValuePair bean, String appId) {
    return NameValuePair.Yaml.Builder.aYaml().withName(bean.getName()).withValue(bean.getValue()).build();
  }

  @Override
  public NameValuePair updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return NameValuePair.Yaml.class;
  }

  @Override
  public NameValuePair get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public NameValuePair update(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }
}
