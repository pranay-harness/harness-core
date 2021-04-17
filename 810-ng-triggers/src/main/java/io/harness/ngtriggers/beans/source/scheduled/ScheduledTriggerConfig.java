package io.harness.ngtriggers.beans.source.scheduled;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.NGTriggerSpec;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@OwnedBy(PIPELINE)
public class ScheduledTriggerConfig implements NGTriggerSpec {
  String type;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = EXTERNAL_PROPERTY, property = "type", visible = true)
  ScheduledTriggerSpec spec;

  @Builder
  public ScheduledTriggerConfig(String type, ScheduledTriggerSpec spec) {
    this.type = type;
    this.spec = spec;
  }
}
