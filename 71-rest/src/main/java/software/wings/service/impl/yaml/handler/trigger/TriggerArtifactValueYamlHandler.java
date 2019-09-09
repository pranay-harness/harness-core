package software.wings.service.impl.yaml.handler.trigger;

import io.harness.eraro.ErrorCode;
import io.harness.exception.TriggerException;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.yaml.trigger.TriggerArtifactSelectionValueYaml;

import java.util.List;

public abstract class TriggerArtifactValueYamlHandler<Y extends TriggerArtifactSelectionValueYaml>
    extends BaseYamlHandler<Y, TriggerArtifactSelectionValue> {
  @Override
  public void delete(ChangeContext<Y> changeContext) {}

  @Override public abstract Y toYaml(TriggerArtifactSelectionValue bean, String appId);

  @Override
  public abstract TriggerArtifactSelectionValue upsertFromYaml(
      ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext);

  @Override
  public TriggerArtifactSelectionValue get(String accountId, String yamlFilePath) {
    throw new TriggerException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }
}
