package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.trigger.Condition;
import software.wings.beans.trigger.PayloadSource;
import software.wings.beans.trigger.WebhookCondition;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.yaml.trigger.PayloadSourceYaml;
import software.wings.yaml.trigger.WebhookConditionYaml;

import java.util.Collections;
import java.util.List;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
public class WebhookConditionYamlHandler extends ConditionYamlHandler<WebhookConditionYaml> {
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Override
  public WebhookConditionYaml toYaml(Condition bean, String appId) {
    WebhookCondition condition = (WebhookCondition) bean;
    PayloadSourceYamlHandler payloadSourceYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.PAYLOAD_SOURCE, condition.getPayloadSource().getType().name());
    PayloadSourceYaml payloadSourceYaml = payloadSourceYamlHandler.toYaml(condition.getPayloadSource(), appId);

    return WebhookConditionYaml.builder().payloadSource(Collections.singletonList(payloadSourceYaml)).build();
  }

  @Override
  public Condition upsertFromYaml(
      ChangeContext<WebhookConditionYaml> changeContext, List<ChangeContext> changeSetContext) {
    WebhookConditionYaml yaml = changeContext.getYaml();

    PayloadSourceYamlHandler payloadSourceYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.PAYLOAD_SOURCE, yaml.getType());
    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, yaml);

    PayloadSource payloadSource = payloadSourceYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
    return WebhookCondition.builder().payloadSource(payloadSource).build();
  }

  @Override
  public Class getYamlClass() {
    return WebhookConditionYaml.class;
  }
}
