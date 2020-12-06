package software.wings.beans;

import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala on 05/28/19
 */
@Value
@Builder
public class TechStack {
  private String category;
  private String technology;
}
