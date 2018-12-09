package io.harness.event.model;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rktummala
 */
@Data
@Builder
public class EventData {
  private Map<String, String> properties = new HashMap<>();
}
