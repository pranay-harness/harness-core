package io.harness.event.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.queue.Queuable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class QueableEvent extends Queuable {
  private Event event;
}
