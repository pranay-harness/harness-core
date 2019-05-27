package software.wings.yaml.trigger;

import com.google.inject.Singleton;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.trigger.TriggerConditionYamlHandler;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
public class WebhookTriggerConditionHandler extends TriggerConditionYamlHandler<WebhookEventTriggerConditionYaml> {
  @Override
  public WebhookEventTriggerConditionYaml toYaml(TriggerCondition bean, String appId) {
    return null;
  }

  @Override
  public TriggerCondition upsertFromYaml(
      ChangeContext<WebhookEventTriggerConditionYaml> changeContext, List<ChangeContext> changeSetContext) {
    return null;
  }

  @Override
  public Class getYamlClass() {
    return null;
  }
}
