package io.harness.pms.notification.bean;

import io.harness.pms.notification.PipelineEventType;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PipelineEvent {
  PipelineEventType type;
  List<String> forStages;
}
