package io.harness.event.model.marketo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

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
