package io.harness.ngtriggers.beans.source.cron;

import io.harness.ngtriggers.beans.source.NGTriggerSpec;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeName("Cron")
public class CronTriggerConfig implements NGTriggerSpec {
  String type;
  String expression;

  @Builder
  public CronTriggerConfig(String type, String expression) {
    this.type = type;
    this.expression = expression;
  }
}
