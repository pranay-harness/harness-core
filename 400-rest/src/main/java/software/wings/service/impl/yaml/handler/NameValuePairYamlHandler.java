package software.wings.service.impl.yaml.handler;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.NameValuePair;
import software.wings.beans.NameValuePairYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 10/28/17
 */
@Singleton
public class NameValuePairYamlHandler extends BaseYamlHandler<NameValuePairYaml, NameValuePair> {
  private NameValuePair toBean(ChangeContext<NameValuePairYaml> changeContext) {
    NameValuePairYaml yaml = changeContext.getYaml();
    return NameValuePair.builder().name(yaml.getName()).value(yaml.getValue()).valueType(yaml.getValueType()).build();
  }

  @Override
  public NameValuePairYaml toYaml(NameValuePair bean, String appId) {
    return NameValuePairYaml.builder()
        .name(bean.getName())
        .value(bean.getValue())
        .valueType(bean.getValueType())
        .build();
  }

  @Override
  public NameValuePair upsertFromYaml(
      ChangeContext<NameValuePairYaml> changeContext, List<ChangeContext> changeSetContext) {
    return toBean(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return NameValuePairYaml.class;
  }

  @Override
  public NameValuePair get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<NameValuePairYaml> changeContext) {
    // Do nothing
  }
}
