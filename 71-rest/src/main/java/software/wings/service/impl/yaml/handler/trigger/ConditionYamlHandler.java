package software.wings.service.impl.yaml.handler.trigger;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.TriggerException;
import software.wings.beans.trigger.Condition;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.yaml.trigger.ConditionYaml;

import java.util.List;

public abstract class ConditionYamlHandler<Y extends ConditionYaml> extends BaseYamlHandler<Y, Condition> {
  @Inject protected YamlHelper yamlHelper;
  @Override
  public void delete(ChangeContext<Y> changeContext) {}
  @Override
  public Condition get(String accountId, String yamlFilePath) {
    throw new TriggerException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override public abstract Y toYaml(Condition bean, String appId);

  @Override
  public abstract Condition upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext);
}
