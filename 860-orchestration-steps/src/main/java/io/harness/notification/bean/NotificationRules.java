package io.harness.notification.bean;

import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationRules {
  String name;
  boolean enabled;

  List<PipelineEvent> pipelineEvents;

  @JsonProperty("notificationMethod") ParameterField<NotificationChannelWrapper> notificationChannelWrapper;
}
