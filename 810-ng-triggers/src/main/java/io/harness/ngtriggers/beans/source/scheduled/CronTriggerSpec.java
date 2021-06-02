package io.harness.ngtriggers.beans.source.scheduled;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CronTriggerSpec implements ScheduledTriggerSpec {
  String expression;
}
