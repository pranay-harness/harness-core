package software.wings.service.impl.yaml.handler.governance;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.governance.EnvironmentFilter;
import io.harness.governance.EnvironmentFilterYaml;

import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import java.util.List;

public abstract class EnvironmentFilterYamlHandler<Y extends EnvironmentFilterYaml, B extends EnvironmentFilter>
    extends BaseYamlHandler<Y, B> {
  @Override
  public void delete(ChangeContext<Y> changeContext) {}

  @Override
  public B get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }
  @Override public abstract Y toYaml(B bean, String accountId);
  @Override public abstract B upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext);
}
