package io.harness.serializer.spring;

import io.harness.ngtriggers.beans.config.HeaderConfig;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.metadata.NGTriggerMetadata;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import io.harness.ngtriggers.beans.target.TargetType;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

public class NGTriggerAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("io.harness.ngtriggers.beans.source.ngTriggerType", NGTriggerType.class);
    orchestrationElements.put("io.harness.ngtriggers.beans.target.targetType", TargetType.class);
    orchestrationElements.put("io.harness.ngtriggers.beans.source.metadata.ngTriggerMetadata", NGTriggerMetadata.class);
    orchestrationElements.put("io.harness.ngtriggers.beans.entity.ngTriggerEntity", NGTriggerEntity.class);
    orchestrationElements.put("io.harness.ngtriggers.beans.entity.ngTriggerWebhookEvent", TriggerWebhookEvent.class);
    orchestrationElements.put("io.harness.ngtriggers.beans.config.headerConfig", HeaderConfig.class);
  }
}
