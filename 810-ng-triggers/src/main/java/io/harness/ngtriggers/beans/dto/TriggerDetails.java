package io.harness.ngtriggers.beans.dto;

import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV1;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TriggerDetails {
  private NGTriggerEntity ngTriggerEntity;
  private NGTriggerConfig ngTriggerConfig;
  private NGTriggerConfigV1 ngTriggerConfigV1;
}
