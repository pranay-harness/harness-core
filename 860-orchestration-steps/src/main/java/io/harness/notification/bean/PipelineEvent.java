package io.harness.notification.bean;

import io.harness.notification.PipelineEventType;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PipelineEvent {
  PipelineEventType type;
  List<String> forStages;
}
