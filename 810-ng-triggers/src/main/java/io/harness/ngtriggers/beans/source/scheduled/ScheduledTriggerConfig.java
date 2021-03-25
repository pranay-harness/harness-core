package io.harness.ngtriggers.beans.source.scheduled;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import io.harness.ngtriggers.beans.source.NGTriggerSpec;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeName("Scheduled")
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
