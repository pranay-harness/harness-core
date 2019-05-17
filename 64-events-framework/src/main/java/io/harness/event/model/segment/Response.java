package io.harness.event.model.segment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala
 */

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {
  private boolean success;
}
