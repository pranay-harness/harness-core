package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 06/19/19
 */

@Data
@Builder
@OwnedBy(PL)
public class UrlInfo {
  private String title;
  private String url;
}
