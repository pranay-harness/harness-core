package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.Data;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.yaml.trigger.TriggerConditionYaml;

import java.util.List;

@OwnedBy(CDC)
@Data
public abstract class TriggerConditionYamlHandler<Y extends TriggerConditionYaml>
    extends BaseYamlHandler<Y, TriggerCondition> {
  @Inject protected YamlHelper yamlHelper;
  @Override
  public void delete(ChangeContext<Y> changeContext) {}
  @Override
  public TriggerCondition get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override public abstract Y toYaml(TriggerCondition bean, String appId);

  @Override
  public abstract TriggerCondition upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext);
}
