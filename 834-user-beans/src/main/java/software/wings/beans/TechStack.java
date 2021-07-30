package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala on 05/28/19
 */
@Value
@Builder
@OwnedBy(PL)
public class TechStack {
  private String category;
  private String technology;
}
