package io.harness.event.model.marketo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author rktummala on 11/20/2018
 */
@Data
@Builder
public class Campaign {
  private Input input;

  @Data
  @Builder
  public static class Input {
    private List<Id> leads;
  }
}
